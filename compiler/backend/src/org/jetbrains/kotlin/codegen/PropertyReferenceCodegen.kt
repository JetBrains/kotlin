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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.codegen.AsmUtil.method
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.context.ClassContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.PropertyImportedFromObject
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.isUnderlyingPropertyOfInlineClass
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.*
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.util.OperatorNameConventions
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
    private val localVariableDescriptorForReference: VariableDescriptor,
    private val target: VariableDescriptor,
    private val boundReceiverJvmKotlinType: JvmKotlinType?
) : MemberCodegen<KtElement>(state, parentCodegen, context, expression, classBuilder) {

    private val boundReceiverType = boundReceiverJvmKotlinType?.type

    private val classDescriptor = context.contextDescriptor
    private val asmType = typeMapper.mapClass(classDescriptor)

    // e.g. MutablePropertyReference0
    private val superAsmType = typeMapper.mapClass(classDescriptor.getSuperClassNotAny().sure { "No super class for $classDescriptor" })

    private val isLocalDelegatedProperty = target is LocalVariableDescriptor

    private val getFunction =
        if (isLocalDelegatedProperty)
            (localVariableDescriptorForReference as VariableDescriptorWithAccessors).getter!!
        else
            findGetFunction(localVariableDescriptorForReference).original

    // e.g. mutableProperty0(Lkotlin/jvm/internal/MutablePropertyReference0;)Lkotlin/reflect/KMutableProperty0;
    private val wrapperMethod = getWrapperMethodForPropertyReference(target, getFunction.valueParameters.size)

    private val closure = bindingContext.get(CodegenBinding.CLOSURE, classDescriptor)!!.apply {
        assert((capturedReceiverFromOuterContext != null) == (boundReceiverType != null)) {
            "Bound property reference can only be generated with the type of the receiver. " +
                    "Captured type = $capturedReceiverFromOuterContext, actual type = $boundReceiverType"
        }
    }

    private val constructorArgs =
        ClosureCodegen.calculateConstructorParameters(typeMapper, state.languageVersionSettings, closure, asmType).apply {
            assert(size <= 1) { "Bound property reference should capture only one value: $this" }
        }

    private val constructor = method("<init>", Type.VOID_TYPE, *constructorArgs.map { it.fieldType }.toTypedArray())

    override fun generateDeclaration() {
        v.defineClass(
            element,
            state.config.classFileVersion,
            ACC_FINAL or ACC_SUPER or
                    DescriptorAsmUtil.getVisibilityAccessFlagForClass(classDescriptor) or
                    DescriptorAsmUtil.getSyntheticAccessFlagForLambdaClass(classDescriptor),
            asmType.internalName,
            null,
            superAsmType.internalName,
            emptyArray()
        )

        v.visitSource(element.containingFile.name, null)
    }

    // TODO: ImplementationBodyCodegen.markLineNumberForSyntheticFunction?
    override fun generateBody() {
        if (JvmCodegenUtil.isConst(closure)) {
            generateConstInstance(asmType, wrapperMethod.returnType)
        } else {
            DescriptorAsmUtil.genClosureFields(closure, v, typeMapper, state.languageVersionSettings)
        }

        generateConstructor()

        if (!isOptimizedPropertyReferenceSupertype(superAsmType)) {
            generateMethod("property reference getName", ACC_PUBLIC, method("getName", JAVA_STRING_TYPE)) {
                aconst(target.name.asString())
            }
            generateMethod("property reference getSignature", ACC_PUBLIC, method("getSignature", JAVA_STRING_TYPE)) {
                generatePropertyReferenceSignature(this, target, state)
            }
            generateMethod("property reference getOwner", ACC_PUBLIC, method("getOwner", K_DECLARATION_CONTAINER_TYPE)) {
                generateCallableReferenceDeclarationContainer(this, target, state)
            }
        }

        if (!isLocalDelegatedProperty) {
            generateAccessors()
        }
    }

    private fun generateConstructor() {
        generateMethod("property reference init", 0, constructor) {
            val shouldHaveBoundReferenceReceiver = closure.isForBoundCallableReference()
            val receiverIndexAndFieldInfo = generateClosureFieldsInitializationFromParameters(closure, constructorArgs)

            load(0, OBJECT_TYPE)
            val superCtorArgTypes = mutableListOf<Type>()
            if (receiverIndexAndFieldInfo != null) {
                val (receiverIndex, receiverFieldInfo) = receiverIndexAndFieldInfo
                loadBoundReferenceReceiverParameter(receiverIndex, receiverFieldInfo.fieldType, receiverFieldInfo.fieldKotlinType)
                superCtorArgTypes.add(OBJECT_TYPE)
            } else {
                assert(!shouldHaveBoundReferenceReceiver) { "No bound reference receiver in constructor parameters: $constructorArgs" }
            }

            if (isOptimizedPropertyReferenceSupertype(superAsmType)) {
                generateCallableReferenceDeclarationContainerClass(this, target, state)
                aconst(target.name.asString())
                generatePropertyReferenceSignature(this, target, state)
                aconst(getCallableReferenceTopLevelFlag(target))
                superCtorArgTypes.add(JAVA_CLASS_TYPE)
                superCtorArgTypes.add(JAVA_STRING_TYPE)
                superCtorArgTypes.add(JAVA_STRING_TYPE)
                superCtorArgTypes.add(Type.INT_TYPE)
            }

            invokespecial(
                superAsmType.internalName, "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE, *superCtorArgTypes.toTypedArray()), false
            )
        }
    }

    private fun generateAccessors() {
        val getFunction = findGetFunction(localVariableDescriptorForReference)
        val getImpl = createFakeOpenDescriptor(getFunction, classDescriptor)
        functionCodegen.generateMethod(
            JvmDeclarationOrigin.NO_ORIGIN,
            getImpl,
            PropertyReferenceGenerationStrategy(
                true,
                getFunction,
                target,
                asmType,
                boundReceiverJvmKotlinType,
                element,
                state,
                false
            )
        )

        if (!ReflectionTypes.isNumberedKMutablePropertyType(localVariableDescriptorForReference.type)) return
        val setFunction = localVariableDescriptorForReference.type.memberScope.getContributedFunctions(
            OperatorNameConventions.SET,
            NoLookupLocation.FROM_BACKEND
        ).single()
        val setImpl = createFakeOpenDescriptor(setFunction, classDescriptor)
        functionCodegen.generateMethod(
            JvmDeclarationOrigin.NO_ORIGIN,
            setImpl,
            PropertyReferenceGenerationStrategy(
                false,
                setFunction,
                target,
                asmType,
                boundReceiverJvmKotlinType,
                element,
                state,
                false
            )
        )
    }


    private fun generateMethod(debugString: String, access: Int, method: Method, generate: InstructionAdapter.() -> Unit) {
        v.generateMethod(debugString, access, method, element, JvmDeclarationOrigin.NO_ORIGIN, state, generate)
    }

    override fun generateKotlinMetadataAnnotation() {
        writeSyntheticClassMetadata(v, state.config, InlineUtil.isInPublicInlineScope(classDescriptor))
    }

    fun putInstanceOnStack(receiverValue: StackValue?): StackValue {
        return StackValue.operation(wrapperMethod.returnType) { iv ->
            if (JvmCodegenUtil.isConst(closure)) {
                assert(receiverValue == null) { "No receiver expected for unbound property reference: $classDescriptor" }
                iv.getstatic(asmType.internalName, JvmAbi.INSTANCE_FIELD, wrapperMethod.returnType.descriptor)
            } else {
                assert(receiverValue != null) { "Receiver expected for bound property reference: $classDescriptor" }
                iv.anew(asmType)
                iv.dup()
                receiverValue!!.put(receiverValue.type, receiverValue.kotlinType, iv)
                iv.invokespecial(asmType.internalName, "<init>", constructor.descriptor, false)
            }
        }
    }

    companion object {
        @JvmField
        val ANY_SUBSTITUTOR = TypeSubstitutor.create(object : TypeSubstitution() {
            override fun get(key: KotlinType): TypeProjection? {
                if (KotlinBuiltIns.isUnit(key)) {
                    return TypeProjectionImpl(key)
                }
                return TypeProjectionImpl(key.builtIns.nullableAnyType)
            }
        })

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

        @JvmStatic
        fun createFakeOpenDescriptor(getFunction: FunctionDescriptor, classDescriptor: ClassDescriptor): FunctionDescriptor {
            val copy = getFunction.original.copy(classDescriptor, Modality.OPEN, getFunction.visibility, getFunction.kind, false)
            return copy.substitute(ANY_SUBSTITUTOR)!!
        }

        @JvmStatic
        fun findGetFunction(localVariableDescriptorForReference: VariableDescriptor) =
            localVariableDescriptorForReference.type.memberScope.getContributedFunctions(
                OperatorNameConventions.GET,
                NoLookupLocation.FROM_BACKEND
            ).single()
    }

    class PropertyReferenceGenerationStrategy(
        private val isGetter: Boolean,
        private val originalFunctionDesc: FunctionDescriptor,
        private val target: VariableDescriptor,
        private val asmType: Type,
        boundReceiverJvmKotlinType: JvmKotlinType?,
        private val expression: KtElement,
        state: GenerationState,
        private val isInliningStrategy: Boolean
    ) :
        FunctionGenerationStrategy.CodegenBased(state) {

        private val boundReceiverType = boundReceiverJvmKotlinType?.type
        private val boundReceiverKotlinType = boundReceiverJvmKotlinType?.kotlinType

        private val expectedReceiverKotlinType =
            target.extensionReceiverParameter?.type
                ?: (target.containingDeclaration as? ClassDescriptor)?.defaultType

        private val expectedReceiverType =
            if (expectedReceiverKotlinType != null)
                state.typeMapper.mapType(expectedReceiverKotlinType)
            else {
                assert(boundReceiverType == null) {
                    "$target: no expected receiver, boundReceiverType is $boundReceiverType"
                }
                null
            }

        override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
            val v = codegen.v
            val typeMapper = state.typeMapper
            val targetKotlinType = target.type

            if (target is PropertyImportedFromObject) {
                val containingObject = target.containingObject
                StackValue
                    .singleton(containingObject, typeMapper)
                    .put(typeMapper.mapClass(containingObject), containingObject.defaultType, v)
            }

            if (boundReceiverType != null) {
                capturedBoundReferenceReceiver(asmType, boundReceiverType, boundReceiverKotlinType, isInliningStrategy)
                    .put(expectedReceiverType!!, expectedReceiverKotlinType, v)
            } else {
                val receivers = originalFunctionDesc.valueParameters.dropLast(if (isGetter) 0 else 1)
                receivers.forEachIndexed { i, valueParameterDescriptor ->
                    val nullableAny = valueParameterDescriptor.builtIns.nullableAnyType
                    StackValue.local(i + 1, OBJECT_TYPE, nullableAny)
                        .put(typeMapper.mapType(valueParameterDescriptor), valueParameterDescriptor.type, v)
                }
            }

            val value = when {
                target is LocalVariableDescriptor -> codegen.findLocalOrCapturedValue(target)!!

                target.isUnderlyingPropertyOfInlineClass() -> {
                    if (expectedReceiverType == null)
                        throw AssertionError("$target: boundReceiverType=$boundReceiverType, expectedReceiverType is null")

                    val receiver =
                        if (boundReceiverType != null)
                            StackValue.onStack(expectedReceiverType, expectedReceiverKotlinType)
                        else
                            StackValue.none()

                    StackValue.underlyingValueOfInlineClass(typeMapper.mapType(targetKotlinType), targetKotlinType, receiver)
                }

                else -> codegen.intermediateValueForProperty(target as PropertyDescriptor, false, null, StackValue.none())
            }

            codegen.markStartLineNumber(expression)

            if (isGetter) {
                value.put(OBJECT_TYPE, targetKotlinType, v)
            } else {
                value.store(
                    StackValue.local(
                        codegen.frameMap.getIndex(
                            codegen.context.functionDescriptor.valueParameters.last()
                        ),
                        OBJECT_TYPE, targetKotlinType
                    ),
                    v
                )
            }
            v.areturn(signature.returnType)
        }
    }
}

