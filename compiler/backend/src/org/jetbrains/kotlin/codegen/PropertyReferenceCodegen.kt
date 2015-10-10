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
import org.jetbrains.kotlin.codegen.AsmUtil.writeKotlinSyntheticClassAnnotation
import org.jetbrains.kotlin.codegen.context.ClassContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetCallableReferenceExpression
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.PropertyImportedFromObject
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.*
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.scopes.receivers.ScriptReceiver
import org.jetbrains.kotlin.utils.sure
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_FINAL
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_SUPER
import org.jetbrains.org.objectweb.asm.Opcodes.V1_6
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method

public class PropertyReferenceCodegen(
        state: GenerationState,
        parentCodegen: MemberCodegen<*>,
        context: ClassContext,
        expression: JetCallableReferenceExpression,
        classBuilder: ClassBuilder,
        private val classDescriptor: ClassDescriptor,
        private val resolvedCall: ResolvedCall<VariableDescriptor>
) : MemberCodegen<JetCallableReferenceExpression>(state, parentCodegen, context, expression, classBuilder) {
    private val target = resolvedCall.getResultingDescriptor()
    private val asmType = typeMapper.mapClass(classDescriptor)

    // e.g. MutablePropertyReference0
    private val superAsmType = typeMapper.mapClass(classDescriptor.getSuperClassNotAny().sure { "No super class for $classDescriptor" })

    // e.g. mutableProperty0(Lkotlin/jvm/internal/MutablePropertyReference0;)Lkotlin/reflect/KMutableProperty0;
    private val wrapperMethod: Method

    init {
        val hasReceiver = target.getDispatchReceiverParameter() != null || target.getExtensionReceiverParameter() != null
        val isMutable = target.isVar()

        wrapperMethod = when {
            hasReceiver -> when {
                isMutable -> method("mutableProperty1", K_MUTABLE_PROPERTY1_TYPE, MUTABLE_PROPERTY_REFERENCE1)
                else -> method("property1", K_PROPERTY1_TYPE, PROPERTY_REFERENCE1)
            }
            else -> when {
                isMutable -> method("mutableProperty0", K_MUTABLE_PROPERTY0_TYPE, MUTABLE_PROPERTY_REFERENCE0)
                else -> method("property0", K_PROPERTY0_TYPE, PROPERTY_REFERENCE0)
            }
        }
    }

    override fun generateDeclaration() {
        v.defineClass(
                element,
                V1_6,
                ACC_FINAL or ACC_SUPER or AsmUtil.getVisibilityAccessFlagForAnonymous(classDescriptor),
                asmType.getInternalName(),
                null,
                superAsmType.getInternalName(),
                emptyArray()
        )

        v.visitSource(element.getContainingFile().getName(), null)
    }

    // TODO: ImplementationBodyCodegen.markLineNumberForSyntheticFunction?
    override fun generateBody() {
        generateConstInstance(asmType, wrapperMethod.getReturnType()) { iv ->
            iv.invokestatic(REFLECTION, wrapperMethod.getName(), wrapperMethod.getDescriptor(), false)
        }

        generateMethod("property reference init", 0, method("<init>", Type.VOID_TYPE)) {
            load(0, OBJECT_TYPE)
            invokespecial(superAsmType.getInternalName(), "<init>", "()V", false)
        }

        generateMethod("property reference getOwner", ACC_PUBLIC, method("getOwner", K_DECLARATION_CONTAINER_TYPE)) {
            ClosureCodegen.generateCallableReferenceDeclarationContainer(this, target, typeMapper)
        }

        generateMethod("property reference getName", ACC_PUBLIC, method("getName", JAVA_STRING_TYPE)) {
            aconst(target.getName().asString())
        }

        generateMethod("property reference getSignature", ACC_PUBLIC, method("getSignature", JAVA_STRING_TYPE)) {
            target as PropertyDescriptor

            val getter = target.getGetter() ?: run {
                val defaultGetter = DescriptorFactory.createDefaultGetter(target, Annotations.EMPTY)
                defaultGetter.initialize(target.getType())
                defaultGetter
            }

            val method = typeMapper.mapSignature(getter).getAsmMethod()
            aconst(method.getName() + method.getDescriptor())
        }

        generateAccessors()
    }

    private fun generateAccessors() {
        val dispatchReceiver = resolvedCall.getDispatchReceiver()
        val extensionReceiver = resolvedCall.getExtensionReceiver()
        val receiverType =
                when {
                    dispatchReceiver is ScriptReceiver -> {
                        // TODO: fix receiver for scripts, see ScriptReceiver#getType
                        dispatchReceiver.getDeclarationDescriptor().getClassDescriptor().getDefaultType()
                    }
                    dispatchReceiver.exists() -> dispatchReceiver.getType()
                    extensionReceiver.exists() -> extensionReceiver.getType()
                    else -> null
                }

        fun generateAccessor(method: Method, accessorBody: InstructionAdapter.(StackValue) -> Unit) {
            generateMethod("property reference $method", ACC_PUBLIC, method) {
                // Note: this descriptor is an inaccurate representation of the get/set method. In particular, it has incorrect
                // return type and value parameter types. However, it's created only to be able to use
                // ExpressionCodegen#intermediateValueForProperty, which is poorly coupled with everything else.
                val fakeDescriptor = SimpleFunctionDescriptorImpl.create(
                        classDescriptor, Annotations.EMPTY, Name.identifier(method.getName()), CallableMemberDescriptor.Kind.DECLARATION,
                        SourceElement.NO_SOURCE
                )
                fakeDescriptor.initialize(null, classDescriptor.getThisAsReceiverParameter(), emptyList(), emptyList(),
                                          classDescriptor.builtIns.getAnyType(), Modality.OPEN, Visibilities.PUBLIC)

                val fakeCodegen = ExpressionCodegen(
                        this, FrameMap(), OBJECT_TYPE, context.intoFunction(fakeDescriptor), state, this@PropertyReferenceCodegen
                )
                if (target is PropertyImportedFromObject) {
                    val containingObject = target.containingObject
                    StackValue.singleton(containingObject, typeMapper).put(typeMapper.mapClass(containingObject), this)
                }

                val receiver =
                        if (receiverType != null) StackValue.coercion(StackValue.local(1, OBJECT_TYPE), typeMapper.mapType(receiverType))
                        else StackValue.none()
                val value = fakeCodegen.intermediateValueForProperty(target as PropertyDescriptor, false, null, receiver)

                accessorBody(value)
            }
        }

        val getterParameters = if (receiverType != null) arrayOf(OBJECT_TYPE) else emptyArray()
        generateAccessor(method("get", OBJECT_TYPE, *getterParameters)) { value ->
            value.put(OBJECT_TYPE, this)
        }

        if (!target.isVar()) return

        val setterParameters = (getterParameters + arrayOf(OBJECT_TYPE))
        generateAccessor(method("set", Type.VOID_TYPE, *setterParameters)) { value ->
            // Hard-coded 1 or 2 is safe here because there's only java/lang/Object in the signature, no double/long parameters
            value.store(StackValue.local(if (receiverType != null) 2 else 1, OBJECT_TYPE), this)
        }
    }

    private fun generateMethod(debugString: String, access: Int, method: Method, generate: InstructionAdapter.() -> Unit) {
        val mv = v.newMethod(JvmDeclarationOrigin.NO_ORIGIN, access, method.getName(), method.getDescriptor(), null, null)

        if (state.classBuilderMode == ClassBuilderMode.FULL) {
            val iv = InstructionAdapter(mv)
            iv.visitCode()
            iv.generate()
            iv.areturn(method.getReturnType())
            FunctionCodegen.endVisit(mv, debugString, element)
        }
    }

    override fun generateKotlinAnnotation() {
        writeKotlinSyntheticClassAnnotation(v)
    }

    public fun putInstanceOnStack(): StackValue =
            StackValue.operation(wrapperMethod.getReturnType()) { iv ->
                iv.getstatic(asmType.getInternalName(), JvmAbi.INSTANCE_FIELD, wrapperMethod.getReturnType().getDescriptor())
            }
}
