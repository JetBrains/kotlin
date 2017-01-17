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
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.context.ClosureContext
import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.coroutines.isSuspendLambda
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.singletonOrEmptyList
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method


class CoroutineCodegen(
        outerExpressionCodegen: ExpressionCodegen,
        element: KtElement,
        private val closureContext: ClosureContext,
        classBuilder: ClassBuilder,
        private val originalSuspendLambdaDescriptor: FunctionDescriptor?
) : ClosureCodegen(
        outerExpressionCodegen.state,
        element, null, closureContext, null,
        FailingFunctionGenerationStrategy,
        outerExpressionCodegen.parentCodegen, classBuilder
) {

    private val classDescriptor = closureContext.contextDescriptor
    private val builtIns = funDescriptor.builtIns

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
                                      builtIns.nullableAnyType,
                                      /* isDefault = */ false, /* isCrossinline = */ false,
                                      /* isNoinline = */ false,
                                      /* varargElementType = */ null, SourceElement.NO_SOURCE
                              ),
                              ValueParameterDescriptorImpl(
                                      this@doResume, null, 1, Annotations.EMPTY, Name.identifier("throwable"),
                                      builtIns.throwable.defaultType.makeNullable(),
                                      /* isDefault = */ false, /* isCrossinline = */ false,
                                      /* isNoinline = */ false,
                                      /* varargElementType = */ null, SourceElement.NO_SOURCE
                              )
                        ),
                        builtIns.nullableAnyType,
                        Modality.FINAL,
                        Visibilities.PUBLIC
                )
            }

    private val createCoroutineDescriptor =
        funDescriptor.createCustomCopy {
            setName(Name.identifier(SUSPEND_FUNCTION_CREATE_METHOD_NAME))
            setReturnType(
                    KotlinTypeFactory.simpleNotNullType(
                            Annotations.EMPTY,
                            builtIns.continuationClassDescriptor,
                            listOf(builtIns.unitType.asTypeProjection())
                    )
            )
            setVisibility(Visibilities.PUBLIC)
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

    override fun generateBridges() {
        if (originalSuspendLambdaDescriptor == null) return
        super.generateBridges()
    }

    override fun generateBody() {
        super.generateBody()

        if (originalSuspendLambdaDescriptor == null) return

        // create() = ...
        functionCodegen.generateMethod(JvmDeclarationOrigin.NO_ORIGIN, createCoroutineDescriptor,
                                       object : FunctionGenerationStrategy.CodegenBased(state) {
                                           override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                                               generateCreateCoroutineMethod(codegen)
                                           }
                                       })

        // invoke(..) = create(..).resume(Unit)
        functionCodegen.generateMethod(JvmDeclarationOrigin.NO_ORIGIN, funDescriptor,
                                       object : FunctionGenerationStrategy.CodegenBased(state) {
                                           override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                                               codegen.v.generateInvokeMethod(signature)
                                           }
                                       })

        if (allLambdaParameters().size <= 1) {
            val delegate = typeMapper.mapSignatureSkipGeneric(createCoroutineDescriptor).asmMethod

            val bridgeParameters = (1..delegate.argumentTypes.size - 1).map { AsmTypes.OBJECT_TYPE } + delegate.argumentTypes.last()
            val bridge = Method(delegate.name, delegate.returnType, bridgeParameters.toTypedArray())

            generateBridge(bridge, delegate)
        }
    }

    private fun InstructionAdapter.generateInvokeMethod(signature: JvmMethodSignature) {
        // this
        load(0, AsmTypes.OBJECT_TYPE)
        val parameterTypes = signature.valueParameters.map { it.asmType }
        parameterTypes.withVariableIndices().forEach {
            (index, type) -> load(index + 1, type)
        }

        // this.create(..)
        invokevirtual(
                v.thisName,
                createCoroutineDescriptor.name.identifier,
                Type.getMethodDescriptor(
                        AsmTypes.CONTINUATION,
                        *parameterTypes.toTypedArray()
                ),
                false
        )
        checkcast(Type.getObjectType(v.thisName))

        // .doResume(Unit)
        invokeDoResumeWithUnit(v.thisName)
        areturn(AsmTypes.OBJECT_TYPE)
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

    private fun generateCreateCoroutineMethod(codegen: ExpressionCodegen) {
        assert(originalSuspendLambdaDescriptor != null) { "create method should only be generated for suspend lambdas" }
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
            originalSuspendLambdaDescriptor?.extensionReceiverParameter.singletonOrEmptyList() + originalSuspendLambdaDescriptor?.valueParameters.orEmpty()

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

    override fun generateKotlinMetadataAnnotation() {
        if (originalSuspendLambdaDescriptor != null) {
            super.generateKotlinMetadataAnnotation()
        }
        else {
            writeKotlinMetadata(v, state, KotlinClassHeader.Kind.SYNTHETIC_CLASS, 0) {
                // Do not write method metadata for raw coroutine state machines
            }
        }
    }

    companion object {

        @JvmStatic
        fun createByLambda(
                expressionCodegen: ExpressionCodegen,
                originalCoroutineLambdaDescriptor: FunctionDescriptor,
                declaration: KtElement,
                classBuilder: ClassBuilder
        ): ClosureCodegen? {
            if (declaration !is KtFunctionLiteral) return null
            if (!originalCoroutineLambdaDescriptor.isSuspendLambda) return null

            return CoroutineCodegen(
                    expressionCodegen,
                    declaration,
                    expressionCodegen.context.intoCoroutineClosure(
                            getOrCreateJvmSuspendFunctionView(originalCoroutineLambdaDescriptor, expressionCodegen.state.bindingContext),
                            originalCoroutineLambdaDescriptor, expressionCodegen, expressionCodegen.state.typeMapper
                    ),
                    classBuilder,
                    originalCoroutineLambdaDescriptor
            )
        }

        fun create(
                expressionCodegen: ExpressionCodegen,
                originalSuspendDescriptor: FunctionDescriptor,
                declaration: KtFunction,
                state: GenerationState
        ): CoroutineCodegen {
            val cv = state.factory.newVisitor(
                    OtherOrigin(declaration, originalSuspendDescriptor),
                    CodegenBinding.asmTypeForAnonymousClass(state.bindingContext, originalSuspendDescriptor),
                    declaration.containingFile
            )

            return CoroutineCodegen(
                    expressionCodegen, declaration,
                    expressionCodegen.context.intoClosure(
                            originalSuspendDescriptor, expressionCodegen, expressionCodegen.state.typeMapper
                    ),
                    cv,
                    originalSuspendLambdaDescriptor = null
            )
        }
    }
}

private const val COROUTINE_LAMBDA_PARAMETER_PREFIX = "p$"

private object FailingFunctionGenerationStrategy : FunctionGenerationStrategy() {
    override fun generateBody(
            mv: MethodVisitor,
            frameMap: FrameMap,
            signature: JvmMethodSignature,
            context: MethodContext,
            parentCodegen: MemberCodegen<*>
    ) {
        error("This functions must not be called")
    }
}
