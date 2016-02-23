/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.codegen.AsmUtil.method
import org.jetbrains.kotlin.codegen.context.ClassContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.PropertyImportedFromObject
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.*
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.utils.sure
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method

class PropertyReferenceCodegen(
        state: GenerationState,
        parentCodegen: MemberCodegen<*>,
        context: ClassContext,
        expression: KtElement,
        classBuilder: ClassBuilder,
        resolvedCall: ResolvedCall<*>
) : MemberCodegen<KtElement>(state, parentCodegen, context, expression, classBuilder) {
    private val classDescriptor = context.contextDescriptor
    private val asmType = typeMapper.mapClass(classDescriptor)

    private val target = resolvedCall.resultingDescriptor as VariableDescriptor
    private val dispatchReceiverType = resolvedCall.dispatchReceiver?.type
    private val extensionReceiverType = target.extensionReceiverParameter?.type

    private val receiverCount =
            (if (dispatchReceiverType != null) 1 else 0) + (if (extensionReceiverType != null) 1 else 0)

    // e.g. MutablePropertyReference0
    private val superAsmType = typeMapper.mapClass(classDescriptor.getSuperClassNotAny().sure { "No super class for $classDescriptor" })

    // e.g. mutableProperty0(Lkotlin/jvm/internal/MutablePropertyReference0;)Lkotlin/reflect/KMutableProperty0;
    private val wrapperMethod = getWrapperMethodForPropertyReference(target, receiverCount)

    override fun generateDeclaration() {
        v.defineClass(
                element,
                V1_6,
                ACC_FINAL or ACC_SUPER or AsmUtil.getVisibilityAccessFlagForClass(classDescriptor),
                asmType.internalName,
                null,
                superAsmType.internalName,
                emptyArray()
        )

        v.visitSource(element.containingFile.name, null)
    }

    // TODO: ImplementationBodyCodegen.markLineNumberForSyntheticFunction?
    override fun generateBody() {
        generateConstInstance(asmType, wrapperMethod.returnType)

        generateMethod("property reference init", 0, method("<init>", Type.VOID_TYPE)) {
            load(0, OBJECT_TYPE)
            invokespecial(superAsmType.internalName, "<init>", "()V", false)
        }

        generateMethod("property reference getOwner", ACC_PUBLIC, method("getOwner", K_DECLARATION_CONTAINER_TYPE)) {
            ClosureCodegen.generateCallableReferenceDeclarationContainer(this, target, state)
        }

        generateMethod("property reference getName", ACC_PUBLIC, method("getName", JAVA_STRING_TYPE)) {
            aconst(target.name.asString())
        }

        generateMethod("property reference getSignature", ACC_PUBLIC, method("getSignature", JAVA_STRING_TYPE)) {
            aconst(getPropertyReferenceSignature(target as PropertyDescriptor, state))
        }

        generateAccessors()
    }

    private fun generateAccessors() {
        fun generateAccessor(method: Method, accessorBody: InstructionAdapter.(StackValue) -> Unit) {
            generateMethod("property reference $method", ACC_PUBLIC, method) {
                // Note: this descriptor is an inaccurate representation of the get/set method. In particular, it has incorrect
                // return type and value parameter types. However, it's created only to be able to use
                // ExpressionCodegen#intermediateValueForProperty, which is poorly coupled with everything else.
                val fakeDescriptor = SimpleFunctionDescriptorImpl.create(
                        classDescriptor, Annotations.EMPTY, Name.identifier(method.name), CallableMemberDescriptor.Kind.DECLARATION,
                        SourceElement.NO_SOURCE
                )
                fakeDescriptor.initialize(null, classDescriptor.thisAsReceiverParameter, emptyList(), emptyList(),
                                          classDescriptor.builtIns.anyType, Modality.OPEN, Visibilities.PUBLIC)

                val fakeCodegen = ExpressionCodegen(
                        this, FrameMap(), OBJECT_TYPE, context.intoFunction(fakeDescriptor), state, this@PropertyReferenceCodegen
                )
                if (target is PropertyImportedFromObject) {
                    val containingObject = target.containingObject
                    StackValue.singleton(containingObject, typeMapper).put(typeMapper.mapClass(containingObject), this)
                }

                for ((index, type) in listOfNotNull(dispatchReceiverType, extensionReceiverType).withIndex()) {
                    StackValue.local(index + 1, OBJECT_TYPE).put(typeMapper.mapType(type), this)
                }

                val value = fakeCodegen.intermediateValueForProperty(target as PropertyDescriptor, false, null, StackValue.none())

                accessorBody(value)
            }
        }

        val getterParameters = (1..receiverCount).map { OBJECT_TYPE }.toTypedArray()
        generateAccessor(method("get", OBJECT_TYPE, *getterParameters)) { value ->
            value.put(OBJECT_TYPE, this)
        }

        if (!target.isVar) return

        val setterParameters = (getterParameters + arrayOf(OBJECT_TYPE))
        generateAccessor(method("set", Type.VOID_TYPE, *setterParameters)) { value ->
            // Number of receivers (not size) is safe here because there's only java/lang/Object in the signature, no double/long parameters
            value.store(StackValue.local(receiverCount + 1, OBJECT_TYPE), this)
        }
    }

    private fun generateMethod(debugString: String, access: Int, method: Method, generate: InstructionAdapter.() -> Unit) {
        val mv = v.newMethod(JvmDeclarationOrigin.NO_ORIGIN, access, method.name, method.descriptor, null, null)

        if (state.classBuilderMode == ClassBuilderMode.FULL) {
            val iv = InstructionAdapter(mv)
            iv.visitCode()
            iv.generate()
            iv.areturn(method.returnType)
            FunctionCodegen.endVisit(mv, debugString, element)
        }
    }

    override fun generateKotlinMetadataAnnotation() {
        writeSyntheticClassMetadata(v)
    }

    fun putInstanceOnStack(): StackValue =
            StackValue.operation(wrapperMethod.returnType) { iv ->
                iv.getstatic(asmType.internalName, JvmAbi.INSTANCE_FIELD, wrapperMethod.returnType.descriptor)
            }

    companion object {
        @JvmStatic
        fun getPropertyReferenceSignature(property: PropertyDescriptor, state: GenerationState): String {
            val getter =
                    property.getter ?: DescriptorFactory.createDefaultGetter(property, Annotations.EMPTY).apply {
                        initialize(property.type)
                    }

            val method = state.typeMapper.mapAsmMethod(getter)
            return method.name + method.descriptor
        }

        @JvmStatic
        fun getWrapperMethodForPropertyReference(property: VariableDescriptor, receiverCount: Int): Method {
            return when (receiverCount) {
                2 -> when {
                    property.isVar -> method("mutableProperty2", K_MUTABLE_PROPERTY2_TYPE, MUTABLE_PROPERTY_REFERENCE2)
                    else -> method("property2", K_PROPERTY2_TYPE, PROPERTY_REFERENCE2)
                }
                1 -> when {
                    property.isVar -> method("mutableProperty1", K_MUTABLE_PROPERTY1_TYPE, MUTABLE_PROPERTY_REFERENCE1)
                    else -> method("property1", K_PROPERTY1_TYPE, PROPERTY_REFERENCE1)
                }
                else -> when {
                    property.isVar -> method("mutableProperty0", K_MUTABLE_PROPERTY0_TYPE, MUTABLE_PROPERTY_REFERENCE0)
                    else -> method("property0", K_PROPERTY0_TYPE, PROPERTY_REFERENCE0)
                }
            }
        }
    }
}
