/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.context.ClosureContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.coroutines.controllerTypeIfCoroutine
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.setSingleOverridden
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method


class CoroutineCodegen(
        state: GenerationState,
        element: KtElement,
        private val closureContext: ClosureContext,
        strategy: FunctionGenerationStrategy,
        parentCodegen: MemberCodegen<*>,
        classBuilder: ClassBuilder,
        private val continuationSuperType: KotlinType,
        private val coroutineLambdaDescriptor: FunctionDescriptor
) : ClosureCodegen(state, element, null, closureContext, null, strategy, parentCodegen, classBuilder) {
    private val controllerType = coroutineLambdaDescriptor.controllerTypeIfCoroutine!!

    override fun generateClosureBody() {
        v.newField(
                JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_PRIVATE or Opcodes.ACC_VOLATILE,
                COROUTINE_CONTROLLER_FIELD_NAME,
                typeMapper.mapType(controllerType).descriptor, null, null)

        v.newField(
                JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_PRIVATE or Opcodes.ACC_VOLATILE,
                COROUTINE_LABEL_FIELD_NAME,
                Type.INT_TYPE.descriptor, null, null)

        for (parameter in funDescriptor.valueParameters) {
            v.newField(
                    OtherOrigin(parameter),
                    Opcodes.ACC_PRIVATE or Opcodes.ACC_FINAL,
                    COROUTINE_LAMBDA_PARAMETER_PREFIX + parameter.index,
                    typeMapper.mapType(parameter.type).descriptor, null, null)
        }

        val classDescriptor = closureContext.contextDescriptor

        val resumeFunctionDescriptor =
                createSynthesizedImplementationByName(
                        "resume",
                        interfaceSupertype = continuationSuperType,
                        implementationClass = classDescriptor,
                        sourceElement = funDescriptor.source)

        val resumeWithExceptionFunctionDescriptor =
                createSynthesizedImplementationByName(
                        "resumeWithException",
                        interfaceSupertype = continuationSuperType,
                        implementationClass = classDescriptor,
                        sourceElement = funDescriptor.source)

        // private fun doResume(result, throwable)
        val combinedResumeFunctionDescriptor =
                resumeFunctionDescriptor.newCopyBuilder()
                        .setVisibility(Visibilities.PRIVATE)
                        .setName(Name.identifier("doResume"))
                        .setCopyOverrides(false)
                        .setPreserveSourceElement()
                        .setValueParameters(
                                listOf(
                                        resumeFunctionDescriptor.valueParameters[0].copy(
                                                resumeFunctionDescriptor, Name.identifier("value"), 0),
                                        resumeWithExceptionFunctionDescriptor.valueParameters[0].copy(
                                                resumeFunctionDescriptor, Name.identifier("exception"), 1))
                        ).build()!!

        generatedDelegationToCombinedResume(resumeFunctionDescriptor, combinedResumeFunctionDescriptor, isSuccess = true)
        generatedDelegationToCombinedResume(resumeWithExceptionFunctionDescriptor, combinedResumeFunctionDescriptor, isSuccess = false)

        functionCodegen.generateMethod(OtherOrigin(element), combinedResumeFunctionDescriptor,
                                       object : FunctionGenerationStrategy.FunctionDefault(state, element as KtDeclarationWithBody) {
                                           override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                                               codegen.v.visitAnnotation(CONTINUATION_METHOD_ANNOTATION_DESC, true).visitEnd()
                                               codegen.initializeCoroutineParameters()
                                               super.doGenerateBody(codegen, signature)
                                               generateExceptionHandlingBlock(codegen)
                                           }
                                       })

        functionCodegen.generateMethod(JvmDeclarationOrigin.NO_ORIGIN, funDescriptor,
                                       object : FunctionGenerationStrategy.CodegenBased(state) {
                                           override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                                                generateInvokeMethod(codegen, signature)
                                           }
                                       })
    }

    private fun generateInvokeMethod(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
        val classDescriptor = closureContext.contextDescriptor
        val owner = typeMapper.mapClass(classDescriptor)
        val controllerFieldInfo = FieldInfo.createForHiddenField(
                owner,
                typeMapper.mapType(controllerType), COROUTINE_CONTROLLER_FIELD_NAME)

        val thisInstance = StackValue.thisOrOuter(codegen, classDescriptor, false, false)

        with(codegen.v) {
            // if (controller != null)
            StackValue.field(controllerFieldInfo, thisInstance).put(AsmTypes.OBJECT_TYPE, this)
            val repeated = Label()
            ifnonnull(repeated)

            // first call
            AsmUtil.genAssignInstanceFieldFromParam(controllerFieldInfo, 1, this)

            setLabelValue(LABEL_VALUE_BEFORE_FIRST_SUSPENSION)

            // Save lambda parameters to fields
            // 0 - this
            // 1 - controller
            var index = 2
            for (parameter in funDescriptor.valueParameters) {
                val fieldInfoForCoroutineLambdaParameter = parameter.getFieldInfoForCoroutineLambdaParameter()
                AsmUtil.genAssignInstanceFieldFromParam(
                        fieldInfoForCoroutineLambdaParameter, index, this)
                index += fieldInfoForCoroutineLambdaParameter.fieldType.size
            }

            load(0, AsmTypes.OBJECT_TYPE)
            areturn(AsmTypes.OBJECT_TYPE)

            // repeated call
            visitLabel(repeated)
            anew(owner)
            dup()

            // pass closure parameters to constructor
            val constructorParameters = calculateConstructorParameters(typeMapper, closure, owner)
            for (parameter in constructorParameters) {
                StackValue.field(parameter, thisInstance).put(parameter.fieldType, this)
            }

            val constructor = Method("<init>", Type.VOID_TYPE, constructorParameters.map { it.fieldType }.toTypedArray())
            invokespecial(owner.internalName, constructor.name, constructor.descriptor, false)

            // Pass lambda parameters to 'invoke' call on newly constructed object
            index = 1
            for (parameter in signature.valueParameters) {
                load(index, parameter.asmType)
                index += parameter.asmType.size
            }

            // 'invoke' call on freshly constructed coroutine returns receiver itself
            invokevirtual(owner.internalName, signature.asmMethod.name, signature.asmMethod.descriptor, false)
            areturn(AsmTypes.OBJECT_TYPE)
        }
    }

    private fun ExpressionCodegen.initializeCoroutineParameters() {
        for (parameter in coroutineLambdaDescriptor.valueParameters) {
            val mappedType = typeMapper.mapType(parameter.type)
            val newIndex = myFrameMap.enter(parameter, mappedType)

            StackValue.field(parameter.getFieldInfoForCoroutineLambdaParameter(), generateThisOrOuter(context.thisDescriptor, false))
                    .put(mappedType, v)
            v.store(newIndex, mappedType)
        }
    }

    private fun ValueParameterDescriptor.getFieldInfoForCoroutineLambdaParameter() =
            FieldInfo.createForHiddenField(
                    typeMapper.mapClass(closureContext.thisDescriptor),
                    typeMapper.mapType(returnType!!),
                    COROUTINE_LAMBDA_PARAMETER_PREFIX + index)

    private fun generateExceptionHandlingBlock(codegen: ExpressionCodegen) {
        val handleExceptionFunction =
                controllerType.memberScope.getContributedFunctions(
                        OperatorNameConventions.COROUTINE_HANDLE_EXCEPTION, KotlinLookupLocation(element)).singleOrNull { it.isOperator }
                        ?: return

        val (resolvedCall, fakeExceptionExpression, fakeThisContinuationException) =
                createResolvedCallForHandleExceptionCall(element, handleExceptionFunction, coroutineLambdaDescriptor)

        codegen.tempVariables.put(fakeExceptionExpression, StackValue.operation(AsmTypes.OBJECT_TYPE) {
            codegen.v.invokestatic(COROUTINE_MARKER_OWNER, HANDLE_EXCEPTION_ARGUMENT_MARKER_NAME, "()Ljava/lang/Object;", false)
        })

        codegen.tempVariables.put(fakeThisContinuationException, codegen.genCoroutineInstanceValueFromResolvedCall(resolvedCall))

        codegen.v.invokestatic(COROUTINE_MARKER_OWNER, HANDLE_EXCEPTION_MARKER_NAME, "()V", false)
        codegen.invokeFunction(resolvedCall, StackValue.none()).put(Type.VOID_TYPE, codegen.v)
        codegen.v.areturn(Type.VOID_TYPE)
    }

    private fun createSynthesizedImplementationByName(
            name: String,
            interfaceSupertype: KotlinType,
            implementationClass: ClassDescriptor,
            sourceElement: SourceElement
    ): SimpleFunctionDescriptor {
        val inSuperType = interfaceSupertype.memberScope.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND).single()
        val result = inSuperType.newCopyBuilder().setSource(sourceElement).setOwner(implementationClass).setModality(Modality.FINAL).build()!!
        result.setSingleOverridden(inSuperType)

        return result
    }

    private fun generatedDelegationToCombinedResume(
            from: SimpleFunctionDescriptor,
            combinedResume: SimpleFunctionDescriptor,
            isSuccess: Boolean
    ) {
        functionCodegen.generateMethod(OtherOrigin(element), from,
                                       object : FunctionGenerationStrategy.CodegenBased(state) {
                                           override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                                               with(codegen.v) {
                                                   load(0, AsmTypes.OBJECT_TYPE)
                                                   if (isSuccess) {
                                                       load(1, AsmTypes.OBJECT_TYPE)
                                                       aconst(null)
                                                   }
                                                   else {
                                                       aconst(null)
                                                       load(1, AsmTypes.OBJECT_TYPE)
                                                   }

                                                   val delegateTo = typeMapper.mapAsmMethod(combinedResume)
                                                   invokevirtual(className, delegateTo.name, delegateTo.descriptor, false)
                                                   areturn(Type.VOID_TYPE)
                                               }
                                           }
                                       })
    }

    private fun InstructionAdapter.setLabelValue(value: Int) {
        load(0, AsmTypes.OBJECT_TYPE)
        iconst(value)
        putfield(v.thisName, COROUTINE_LABEL_FIELD_NAME, Type.INT_TYPE.descriptor)
    }

    override fun generateAdditionalCodeInConstructor(iv: InstructionAdapter) {
        super.generateAdditionalCodeInConstructor(iv)
        // Change label value to illegal to make sure continuation will not be used until `invoke(Controler)`
        // where correct value will be set up
        iv.setLabelValue(LABEL_VALUE_BEFORE_INVOKE)
    }

    companion object {
        private const val LABEL_VALUE_BEFORE_INVOKE = -2
        private const val LABEL_VALUE_BEFORE_FIRST_SUSPENSION = 0

        @JvmStatic fun create(
                expressionCodegen: ExpressionCodegen,
                originalCoroutineLambdaDescriptor: FunctionDescriptor,
                declaration: KtElement,
                classBuilder: ClassBuilder
        ): ClosureCodegen? {
            val classDescriptor = expressionCodegen.bindingContext[CodegenBinding.CLASS_FOR_CALLABLE, originalCoroutineLambdaDescriptor] ?: return null
            declaration as? KtFunction ?: return null

            val continuationSupertype =
                    classDescriptor.typeConstructor.supertypes.firstOrNull {
                        it.constructor.declarationDescriptor?.fqNameUnsafe ==
                                DescriptorUtils.CONTINUATION_INTERFACE_FQ_NAME.toUnsafe()
                    } ?: return null

            val descriptorWithContinuationReturnType =
                    originalCoroutineLambdaDescriptor.newCopyBuilder().setPreserveSourceElement().setReturnType(continuationSupertype).build()!!

            val state = expressionCodegen.state
            return CoroutineCodegen(
                    state,
                    declaration,
                    expressionCodegen.context.intoCoroutineClosure(
                            descriptorWithContinuationReturnType, originalCoroutineLambdaDescriptor, expressionCodegen, state.typeMapper),
                    FunctionGenerationStrategy.FunctionDefault(state, declaration), expressionCodegen.parentCodegen, classBuilder,
                    continuationSupertype,
                    originalCoroutineLambdaDescriptor)
        }
    }
}

private const val COROUTINE_LAMBDA_PARAMETER_PREFIX = "p$"
