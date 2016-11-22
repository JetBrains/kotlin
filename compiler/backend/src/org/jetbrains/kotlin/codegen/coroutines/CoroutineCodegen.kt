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
import org.jetbrains.kotlin.codegen.context.ClosureContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.coroutines.controllerTypeIfCoroutine
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.KotlinLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.makeNullable
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
        private val coroutineLambdaDescriptor: FunctionDescriptor,
        private val controllerType: KotlinType
) : ClosureCodegen(state, element, null, closureContext, null, strategy, parentCodegen, classBuilder) {

    override fun generateClosureBody() {
        for (parameter in funDescriptor.valueParameters) {
            v.newField(
                    OtherOrigin(parameter),
                    Opcodes.ACC_PRIVATE,
                    COROUTINE_LAMBDA_PARAMETER_PREFIX + parameter.index,
                    typeMapper.mapType(parameter.type).descriptor, null, null)
        }

        generateDoResume()

        functionCodegen.generateMethod(JvmDeclarationOrigin.NO_ORIGIN, funDescriptor,
                                       object : FunctionGenerationStrategy.CodegenBased(state) {
                                           override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                                                generateInvokeMethod(codegen, signature)
                                           }
                                       })
    }

    // invoke for lambda being passes to builder
    // fun builder(coroutine c: Controller.() -> Continuation<Unit>)
    //
    // This lambda must have a receiver parameter, may have value parameters and returns Continuation<Unit> (`this` instance or a copy of it)
    private fun generateInvokeMethod(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
        val classDescriptor = closureContext.contextDescriptor
        val owner = typeMapper.mapClass(classDescriptor)
        val controllerFieldInfo =
                FieldInfo.createForHiddenField(
                        AsmTypes.COROUTINE_IMPL,
                        AsmTypes.OBJECT_TYPE, COROUTINE_CONTROLLER_FIELD_NAME
                )

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

            generateLoadField(parameter.getFieldInfoForCoroutineLambdaParameter())
            v.store(newIndex, mappedType)
        }
    }

    private fun ExpressionCodegen.generateLoadField(fieldInfo: FieldInfo) {
        StackValue.field(fieldInfo, generateThisOrOuter(context.thisDescriptor, false)).put(fieldInfo.fieldType, v)
    }

    private fun ValueParameterDescriptor.getFieldInfoForCoroutineLambdaParameter() =
            createHiddenFieldInfo(type, COROUTINE_LAMBDA_PARAMETER_PREFIX + index)

    private fun createHiddenFieldInfo(type: KotlinType, name: String) =
            FieldInfo.createForHiddenField(
                    typeMapper.mapClass(closureContext.thisDescriptor),
                    typeMapper.mapType(type),
                    name
            )

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

    private fun generateDoResume() {
        val classDescriptor = closureContext.contextDescriptor

        // protected fun doResume(result, throwable)
        val doResumeDescriptor =
                SimpleFunctionDescriptorImpl.create(
                        classDescriptor, Annotations.EMPTY, Name.identifier("doResume"), CallableMemberDescriptor.Kind.DECLARATION,
                        funDescriptor.source
                ).apply doResume@{
                    initialize(
                            /* receiverParameterType = */ null,
                            classDescriptor.thisAsReceiverParameter,
                            /* typeParameters =   */ emptyList(),
                            listOf(
                                    ValueParameterDescriptorImpl(
                                          this@doResume, null, 0, Annotations.EMPTY, Name.identifier("data"),
                                          module.builtIns.nullableAnyType,
                                          /* isDefault = */ false, /* isCrossinline = */ false,
                                          /* isNoinline = */ false, /* isCoroutine = */ false,
                                          /* varargElementType = */ null, SourceElement.NO_SOURCE
                                    ),
                                    ValueParameterDescriptorImpl(
                                          this@doResume, null, 1, Annotations.EMPTY, Name.identifier("throwable"),
                                          module.builtIns.throwable.defaultType.makeNullable(),
                                          /* isDefault = */ false, /* isCrossinline = */ false,
                                          /* isNoinline = */ false, /* isCoroutine = */ false,
                                          /* varargElementType = */ null, SourceElement.NO_SOURCE
                                    )
                            ),
                            module.builtIns.unitType,
                            Modality.FINAL,
                            Visibilities.PROTECTED
                    )
                }

        functionCodegen.generateMethod(OtherOrigin(element), doResumeDescriptor,
                                       object : FunctionGenerationStrategy.FunctionDefault(state, element as KtDeclarationWithBody) {
                                           override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                                               codegen.v.visitAnnotation(CONTINUATION_METHOD_ANNOTATION_DESC, true).visitEnd()
                                               codegen.initializeCoroutineParameters()
                                               super.doGenerateBody(codegen, signature)
                                               generateExceptionHandlingBlock(codegen)
                                           }
                                       })
    }

    private fun InstructionAdapter.setLabelValue(value: Int) {
        load(0, AsmTypes.OBJECT_TYPE)
        iconst(value)
        putfield(AsmTypes.COROUTINE_IMPL.internalName, COROUTINE_LABEL_FIELD_NAME, Type.INT_TYPE.descriptor)
    }

    companion object {
        private const val LABEL_VALUE_BEFORE_FIRST_SUSPENSION = 0

        @JvmStatic
        fun create(
                expressionCodegen: ExpressionCodegen,
                originalCoroutineLambdaDescriptor: FunctionDescriptor,
                declaration: KtElement,
                classBuilder: ClassBuilder
        ): ClosureCodegen? {
            if (declaration !is KtFunctionLiteral) return null
            val controllerType = originalCoroutineLambdaDescriptor.controllerTypeIfCoroutine ?: return null

            val descriptorWithContinuationReturnType =
                    originalCoroutineLambdaDescriptor.newCopyBuilder()
                            .setPreserveSourceElement()
                            .setReturnType(expressionCodegen.state.jvmRuntimeTypes.continuationOfAny)
                            .build()!!

            val state = expressionCodegen.state
            return CoroutineCodegen(
                    state,
                    declaration,
                    expressionCodegen.context.intoCoroutineClosure(
                            descriptorWithContinuationReturnType, originalCoroutineLambdaDescriptor, expressionCodegen, state.typeMapper
                    ),
                    FunctionGenerationStrategy.FunctionDefault(state, declaration),
                    expressionCodegen.parentCodegen, classBuilder,
                    originalCoroutineLambdaDescriptor,
                    controllerType
            )
        }
    }
}

private const val COROUTINE_LAMBDA_PARAMETER_PREFIX = "p$"
