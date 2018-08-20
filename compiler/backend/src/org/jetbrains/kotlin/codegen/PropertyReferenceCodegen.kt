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
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.PropertyImportedFromObject
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
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
    private val receiverType: Type?
) : MemberCodegen<KtElement>(state, parentCodegen, context, expression, classBuilder) {
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
        assert((captureReceiverType != null) == (receiverType != null)) {
            "Bound property reference can only be generated with the type of the receiver. " +
                    "Captured type = $captureReceiverType, actual type = $receiverType"
        }
    }

    private val constructorArgs = ClosureCodegen.calculateConstructorParameters(typeMapper, closure, asmType).apply {
        assert(size <= 1) { "Bound property reference should capture only one value: $this" }
    }
    private val constructor = method("<init>", Type.VOID_TYPE, *constructorArgs.map { it.fieldType }.toTypedArray())

    override fun generateDeclaration() {
        v.defineClass(
            element,
            state.classFileVersion,
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
        if (JvmCodegenUtil.isConst(closure)) {
            generateConstInstance(asmType, wrapperMethod.returnType)
        } else {
            AsmUtil.genClosureFields(closure, v, typeMapper)
        }

        generateConstructor()

        generateMethod("property reference getName", ACC_PUBLIC, method("getName", JAVA_STRING_TYPE)) {
            aconst(target.name.asString())
        }

        generateMethod("property reference getSignature", ACC_PUBLIC, method("getSignature", JAVA_STRING_TYPE)) {
            generateCallableReferenceSignature(this, target, state)
        }

        generateMethod("property reference getOwner", ACC_PUBLIC, method("getOwner", K_DECLARATION_CONTAINER_TYPE)) {
            ClosureCodegen.generateCallableReferenceDeclarationContainer(this, target, state)
        }

        if (!isLocalDelegatedProperty) {
            generateAccessors()
        }
    }

    private fun generateConstructor() {
        generateMethod("property reference init", 0, constructor) {
            val shouldHaveBoundReferenceReceiver = closure.isForBoundCallableReference()
            val receiverIndexAndType = generateClosureFieldsInitializationFromParameters(closure, constructorArgs)

            if (receiverIndexAndType == null) {
                assert(!shouldHaveBoundReferenceReceiver) { "No bound reference receiver in constructor parameters: $constructorArgs" }
                load(0, OBJECT_TYPE)
                invokespecial(superAsmType.internalName, "<init>", "()V", false)
            } else {
                val (receiverIndex, receiverType) = receiverIndexAndType
                load(0, OBJECT_TYPE)
                loadBoundReferenceReceiverParameter(receiverIndex, receiverType)
                invokespecial(superAsmType.internalName, "<init>", "(Ljava/lang/Object;)V", false)
            }
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
                receiverType,
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
                receiverType,
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
        writeSyntheticClassMetadata(v, state)
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
        fun generateCallableReferenceSignature(iv: InstructionAdapter, callable: CallableDescriptor, state: GenerationState) {
            iv.aconst(getSignatureString(callable, state))
        }

        @JvmStatic
        fun getSignatureString(callable: CallableDescriptor, state: GenerationState): String {
            if (callable is LocalVariableDescriptor) {
                val asmType = state.bindingContext.get(CodegenBinding.DELEGATED_PROPERTY_METADATA_OWNER, callable)
                    ?: throw AssertionError("No delegated property metadata owner for $callable")
                val localDelegatedProperties = CodegenBinding.getLocalDelegatedProperties(state.bindingContext, asmType)
                val index = localDelegatedProperties?.indexOf(callable) ?: -1
                if (index < 0) {
                    throw AssertionError("Local delegated property is not found in $asmType: $callable")
                }
                return "<v#$index>"
            }

            val accessor = when (callable) {
                is FunctionDescriptor -> callable
                is VariableDescriptorWithAccessors ->
                    callable.getter ?: DescriptorFactory.createDefaultGetter(callable as PropertyDescriptor, Annotations.EMPTY).apply {
                        initialize(callable.type)
                    }
                else -> error("Unsupported callable reference: $callable")
            }
            val declaration = DescriptorUtils.unwrapFakeOverride(accessor).original
            val method = state.typeMapper.mapAsmMethod(declaration)
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
        val isGetter: Boolean,
        private val originalFunctionDesc: FunctionDescriptor,
        val target: VariableDescriptor,
        val asmType: Type,
        val receiverType: Type?,
        val expression: KtElement,
        state: GenerationState,
        private val isInliningStrategy: Boolean
    ) :
        FunctionGenerationStrategy.CodegenBased(state) {
        override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
            val v = codegen.v
            val typeMapper = state.typeMapper
            if (target is PropertyImportedFromObject) {
                val containingObject = target.containingObject
                StackValue
                    .singleton(containingObject, typeMapper)
                    .put(typeMapper.mapClass(containingObject), containingObject.defaultType, v)
            }

            if (receiverType != null) {
                val expectedReceiver =
                    target.extensionReceiverParameter?.type ?: (target.containingDeclaration as? ClassDescriptor)?.defaultType
                val expectedReceiverType = if (expectedReceiver != null) typeMapper.mapType(expectedReceiver) else receiverType
                capturedBoundReferenceReceiver(asmType, expectedReceiverType, isInliningStrategy).put(expectedReceiverType, v)
            } else {
                val receivers = originalFunctionDesc.valueParameters.dropLast(if (isGetter) 0 else 1)
                receivers.forEachIndexed { i, valueParameterDescriptor ->
                    StackValue.local(i + 1, OBJECT_TYPE).put(typeMapper.mapType(valueParameterDescriptor), valueParameterDescriptor.type, v)
                }
            }

            val value = if (target is LocalVariableDescriptor) {
                codegen.findLocalOrCapturedValue(target)!!
            } else {
                codegen.intermediateValueForProperty(target as PropertyDescriptor, false, null, StackValue.none())
            }

            codegen.markStartLineNumber(expression)
            val type = target.type
            if (isGetter) {
                value.put(OBJECT_TYPE, type, v)
            } else {
                val functionDescriptor = codegen.context.functionDescriptor
                value.store(StackValue.local(codegen.frameMap.getIndex(functionDescriptor.valueParameters.last()), OBJECT_TYPE, type), v)
            }
            v.areturn(signature.returnType)
        }
    }
}

