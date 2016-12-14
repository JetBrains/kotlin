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

import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.context.ClosureContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.coroutines.isSuspendLambda
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.singletonOrEmptyList
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
        private val coroutineLambdaDescriptor: FunctionDescriptor
) : ClosureCodegen(state, element, null, closureContext, null, strategy, parentCodegen, classBuilder) {

    private val classDescriptor = closureContext.contextDescriptor

    private lateinit var constructorToUseFromInvoke: Method

    // protected fun doResume(result, throwable)
    private val doResumeDescriptor =
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
                        funDescriptor.builtIns.nullableAnyType,
                        Modality.FINAL,
                        Visibilities.PROTECTED
                )
            }

    override fun generateClosureBody() {
        for (parameter in allLambdaParameters()) {
            val fieldInfo = parameter.getFieldInfoForCoroutineLambdaParameter()
            v.newField(
                    OtherOrigin(parameter),
                    Opcodes.ACC_PRIVATE,
                    fieldInfo.fieldName,
                    fieldInfo.fieldType.descriptor, null, null
            )
        }

        generateDoResume()
    }

    override fun generateBody() {
        super.generateBody()

        functionCodegen.generateMethod(JvmDeclarationOrigin.NO_ORIGIN, funDescriptor,
                                       object : FunctionGenerationStrategy.CodegenBased(state) {
                                           override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                                               generateInvokeMethod(codegen)
                                           }
                                       })
    }

    override fun generateConstructor(): Method {
        val args = calculateConstructorParameters(typeMapper, closure, asmType)
        val argTypes = args.map { it.fieldType }.plus(AsmTypes.CONTINUATION).toTypedArray()

        val constructor = Method("<init>", Type.VOID_TYPE, argTypes)
        val mv = v.newMethod(
                OtherOrigin(element, funDescriptor), visibilityFlag, "<init>", constructor.descriptor, null,
                ArrayUtil.EMPTY_STRING_ARRAY
        )

        constructorToUseFromInvoke = constructor

        if (state.classBuilderMode.generateBodies) {
            mv.visitCode()
            val iv = InstructionAdapter(mv)

            iv.generateClosureFieldsInitializationFromParameters(closure, args)

            iv.load(0, AsmTypes.OBJECT_TYPE)
            iv.iconst(calculateArity())
            iv.load(argTypes.map { it.size }.sum(), AsmTypes.OBJECT_TYPE)

            val superClassConstructorDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, AsmTypes.CONTINUATION)
            iv.invokespecial(superClassAsmType.internalName, "<init>", superClassConstructorDescriptor, false)

            iv.visitInsn(Opcodes.RETURN)

            FunctionCodegen.endVisit(iv, "constructor", element)
        }

        return constructor
    }

    private fun generateInvokeMethod(codegen: ExpressionCodegen) {
        val classDescriptor = closureContext.contextDescriptor
        val owner = typeMapper.mapClass(classDescriptor)

        val thisInstance = StackValue.thisOrOuter(codegen, classDescriptor, false, false)

        with(codegen.v) {
            anew(owner)
            dup()

            // pass captured closure to constructor
            val constructorParameters = calculateConstructorParameters(typeMapper, closure, owner)
            for (parameter in constructorParameters) {
                StackValue.field(parameter, thisInstance).put(parameter.fieldType, this)
            }

            // load resultContinuation
            load(allLambdaParameters().map { typeMapper.mapType(it.type).size }.sum() + 1, AsmTypes.OBJECT_TYPE)

            invokespecial(owner.internalName, constructorToUseFromInvoke.name, constructorToUseFromInvoke.descriptor, false)

            val cloneIndex = codegen.frameMap.enterTemp(AsmTypes.OBJECT_TYPE)
            store(cloneIndex, AsmTypes.OBJECT_TYPE)

            // Pass lambda parameters to 'invoke' call on newly constructed object
            var index = 1
            for (parameter in allLambdaParameters()) {
                val fieldInfoForCoroutineLambdaParameter = parameter.getFieldInfoForCoroutineLambdaParameter()
                load(index, fieldInfoForCoroutineLambdaParameter.fieldType)
                AsmUtil.genAssignInstanceFieldFromParam(fieldInfoForCoroutineLambdaParameter, index, this, cloneIndex)
                index += fieldInfoForCoroutineLambdaParameter.fieldType.size
            }

            load(cloneIndex, AsmTypes.OBJECT_TYPE)
            areturn(AsmTypes.OBJECT_TYPE)
        }
    }

    private fun ExpressionCodegen.initializeCoroutineParameters() {
        for (parameter in allLambdaParameters()) {
            val mappedType = typeMapper.mapType(parameter.type)
            val newIndex = myFrameMap.enter(parameter, mappedType)

            generateLoadField(parameter.getFieldInfoForCoroutineLambdaParameter())
            v.store(newIndex, mappedType)
        }
    }

    private fun allLambdaParameters() =
            coroutineLambdaDescriptor.extensionReceiverParameter.singletonOrEmptyList()

    private fun ExpressionCodegen.generateLoadField(fieldInfo: FieldInfo) {
        StackValue.field(fieldInfo, generateThisOrOuter(context.thisDescriptor, false)).put(fieldInfo.fieldType, v)
    }

    private fun ParameterDescriptor.getFieldInfoForCoroutineLambdaParameter() =
            createHiddenFieldInfo(type, COROUTINE_LAMBDA_PARAMETER_PREFIX + (this.safeAs<ValueParameterDescriptor>()?.index ?: ""))

    private fun createHiddenFieldInfo(type: KotlinType, name: String) =
            FieldInfo.createForHiddenField(
                    typeMapper.mapClass(closureContext.thisDescriptor),
                    typeMapper.mapType(type),
                    name
            )

    private fun generateDoResume() {
        functionCodegen.generateMethod(
                OtherOrigin(element),
                doResumeDescriptor,
                object : FunctionGenerationStrategy.FunctionDefault(state, element as KtDeclarationWithBody) {
                    override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                        codegen.v.visitAnnotation(CONTINUATION_METHOD_ANNOTATION_DESC, true).visitEnd()
                        codegen.initializeCoroutineParameters()
                        super.doGenerateBody(codegen, signature)
                    }
                }
        )
    }

    companion object {

        @JvmStatic
        fun create(
                expressionCodegen: ExpressionCodegen,
                originalCoroutineLambdaDescriptor: FunctionDescriptor,
                declaration: KtElement,
                classBuilder: ClassBuilder
        ): ClosureCodegen? {
            if (declaration !is KtFunctionLiteral) return null
            if (!originalCoroutineLambdaDescriptor.isSuspendLambda) return null

            val descriptorWithContinuationReturnType = createJvmSuspendFunctionView(originalCoroutineLambdaDescriptor)

            val state = expressionCodegen.state
            return CoroutineCodegen(
                    state,
                    declaration,
                    expressionCodegen.context.intoCoroutineClosure(
                            descriptorWithContinuationReturnType, originalCoroutineLambdaDescriptor, expressionCodegen, state.typeMapper
                    ),
                    FunctionGenerationStrategy.FunctionDefault(state, declaration),
                    expressionCodegen.parentCodegen, classBuilder,
                    originalCoroutineLambdaDescriptor
            )
        }
    }
}

private const val COROUTINE_LAMBDA_PARAMETER_PREFIX = "p$"
