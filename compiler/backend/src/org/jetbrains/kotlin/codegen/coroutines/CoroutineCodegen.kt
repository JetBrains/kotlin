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
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.sure
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method


abstract class AbstractCoroutineCodegen(
        outerExpressionCodegen: ExpressionCodegen,
        element: KtElement,
        closureContext: ClosureContext,
        classBuilder: ClassBuilder,
        userDataForDoResume: Map<out FunctionDescriptor.UserDataKey<*>, *>? = null
) : ClosureCodegen(
        outerExpressionCodegen.state,
        element, null, closureContext, null,
        FailingFunctionGenerationStrategy,
        outerExpressionCodegen.parentCodegen, classBuilder
) {
    protected val classDescriptor = closureContext.contextDescriptor

    protected val doResumeDescriptor =
            SimpleFunctionDescriptorImpl.create(
                    classDescriptor, Annotations.EMPTY, Name.identifier(DO_RESUME_METHOD_NAME), CallableMemberDescriptor.Kind.DECLARATION,
                    funDescriptor.source
            ).apply doResume@{
                initialize(
                      null,
                      classDescriptor.thisAsReceiverParameter,
                      emptyList(),
                      listOf(
                              createValueParameterForDoResume(Name.identifier("data"), builtIns.nullableAnyType, 0),
                              createValueParameterForDoResume(Name.identifier("throwable"), builtIns.throwable.defaultType.makeNullable(), 1)
                      ),
                      builtIns.nullableAnyType,
                      Modality.FINAL,
                      Visibilities.PUBLIC,
                      userDataForDoResume
                )
            }

    private fun FunctionDescriptor.createValueParameterForDoResume(name: Name, type: KotlinType, index: Int) =
            ValueParameterDescriptorImpl(
                    this, null, index, Annotations.EMPTY, name,
                    type,
                    false, false,
                    false,
                    null, SourceElement.NO_SOURCE
            )

    override fun generateConstructor(): Method {
        val args = calculateConstructorParameters(typeMapper, closure, asmType)
        val argTypes = args.map { it.fieldType }.plus(CONTINUATION_ASM_TYPE).toTypedArray()

        val constructor = Method("<init>", Type.VOID_TYPE, argTypes)
        val mv = v.newMethod(
                OtherOrigin(element, funDescriptor), visibilityFlag, "<init>", constructor.descriptor, null,
                ArrayUtil.EMPTY_STRING_ARRAY
        )

        if (state.classBuilderMode.generateBodies) {
            mv.visitCode()
            val iv = InstructionAdapter(mv)

            iv.generateClosureFieldsInitializationFromParameters(closure, args)

            iv.load(0, AsmTypes.OBJECT_TYPE)
            iv.iconst(if (passArityToSuperClass) calculateArity() else 0)
            iv.load(argTypes.map { it.size }.sum(), AsmTypes.OBJECT_TYPE)

            val superClassConstructorDescriptor = Type.getMethodDescriptor(
                    Type.VOID_TYPE,
                    Type.INT_TYPE,
                    CONTINUATION_ASM_TYPE
            )
            iv.invokespecial(superClassAsmType.internalName, "<init>", superClassConstructorDescriptor, false)

            iv.visitInsn(Opcodes.RETURN)

            FunctionCodegen.endVisit(iv, "constructor", element)
        }

        return constructor
    }

    abstract protected val passArityToSuperClass: Boolean
}

class CoroutineCodegenForLambda private constructor(
        outerExpressionCodegen: ExpressionCodegen,
        element: KtElement,
        private val closureContext: ClosureContext,
        classBuilder: ClassBuilder,
        private val originalSuspendFunctionDescriptor: FunctionDescriptor
) : AbstractCoroutineCodegen(
        outerExpressionCodegen, element, closureContext, classBuilder,
        userDataForDoResume = mapOf(INITIAL_SUSPEND_DESCRIPTOR_FOR_DO_RESUME to originalSuspendFunctionDescriptor)
) {
    private val builtIns = funDescriptor.builtIns

    private lateinit var constructorToUseFromInvoke: Method

    private val createCoroutineDescriptor =
        funDescriptor.createCustomCopy {
            setName(Name.identifier(SUSPEND_FUNCTION_CREATE_METHOD_NAME))
            setReturnType(
                    funDescriptor.module.getContinuationOfTypeOrAny(builtIns.unitType)
            )
            // 'create' method should not inherit initial descriptor for suspend function from original descriptor
            putUserData(INITIAL_DESCRIPTOR_FOR_SUSPEND_FUNCTION, null)
            setVisibility(Visibilities.PUBLIC)
        }

    override fun generateClosureBody() {
        for (parameter in allFunctionParameters()) {
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

        if (allFunctionParameters().size <= 1) {
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
                        CONTINUATION_ASM_TYPE,
                        *parameterTypes.toTypedArray()
                ),
                false
        )
        checkcast(Type.getObjectType(v.thisName))

        // .doResume(Unit)
        invokeDoResumeWithUnit(v.thisName)
        areturn(AsmTypes.OBJECT_TYPE)
    }

    override val passArityToSuperClass get() = true

    override fun generateConstructor(): Method {
        constructorToUseFromInvoke = super.generateConstructor()
        return constructorToUseFromInvoke
    }

    private fun generateCreateCoroutineMethod(codegen: ExpressionCodegen) {
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
            load(allFunctionParameters().map { typeMapper.mapType(it.type).size }.sum() + 1, AsmTypes.OBJECT_TYPE)

            invokespecial(owner.internalName, constructorToUseFromInvoke.name, constructorToUseFromInvoke.descriptor, false)

            val cloneIndex = codegen.frameMap.enterTemp(AsmTypes.OBJECT_TYPE)
            store(cloneIndex, AsmTypes.OBJECT_TYPE)

            // Pass lambda parameters to 'invoke' call on newly constructed object
            var index = 1
            for (parameter in allFunctionParameters()) {
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
        for (parameter in allFunctionParameters()) {
            val fieldStackValue =
                    StackValue.field(
                            parameter.getFieldInfoForCoroutineLambdaParameter(), generateThisOrOuter(context.thisDescriptor, false)
                    )

            val mappedType = typeMapper.mapType(parameter.type)
            fieldStackValue.put(mappedType, v)

            val newIndex = myFrameMap.enter(parameter, mappedType)
            v.store(newIndex, mappedType)
        }

        initializeVariablesForDestructuredLambdaParameters(this, originalSuspendFunctionDescriptor.valueParameters)
    }

    private fun allFunctionParameters() =
            originalSuspendFunctionDescriptor.extensionReceiverParameter.let(::listOfNotNull) +
            originalSuspendFunctionDescriptor.valueParameters.orEmpty()

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

                    override fun wrapMethodVisitor(mv: MethodVisitor, access: Int, name: String, desc: String): MethodVisitor {
                        return CoroutineTransformerMethodVisitor(
                                mv, access, name, desc, null, null,
                                obtainClassBuilderForCoroutineState = { v },
                                element = element,
                                containingClassInternalName = v.thisName,
                                isForNamedFunction = false
                        )
                    }

                    override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
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
                originalSuspendLambdaDescriptor: FunctionDescriptor,
                declaration: KtElement,
                classBuilder: ClassBuilder
        ): ClosureCodegen? {
            if (declaration !is KtFunctionLiteral || !originalSuspendLambdaDescriptor.isSuspendLambda) return null

            return CoroutineCodegenForLambda(
                    expressionCodegen,
                    declaration,
                    expressionCodegen.context.intoCoroutineClosure(
                            getOrCreateJvmSuspendFunctionView(originalSuspendLambdaDescriptor, expressionCodegen.state.bindingContext),
                            originalSuspendLambdaDescriptor, expressionCodegen, expressionCodegen.state.typeMapper
                    ),
                    classBuilder,
                    originalSuspendLambdaDescriptor
            )
        }
    }
}

class CoroutineCodegenForNamedFunction private constructor(
        outerExpressionCodegen: ExpressionCodegen,
        element: KtElement,
        closureContext: ClosureContext,
        classBuilder: ClassBuilder,
        originalSuspendFunctionDescriptor: FunctionDescriptor
) : AbstractCoroutineCodegen(outerExpressionCodegen, element, closureContext, classBuilder) {
    private val suspendFunctionJvmView =
            bindingContext[CodegenBinding.SUSPEND_FUNCTION_TO_JVM_VIEW, originalSuspendFunctionDescriptor]!!

    override val passArityToSuperClass get() = false

    override fun generateBridges() {
        // Do not generate any closure bridges
    }

    override fun generateClosureBody() {
        generateDoResume()

        generateGetLabelMethod()
        generateSetLabelMethod()

        v.newField(
                JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_SYNTHETIC or AsmUtil.NO_FLAG_PACKAGE_PRIVATE,
                DATA_FIELD_NAME, AsmTypes.OBJECT_TYPE.descriptor, null, null
        )
        v.newField(
                JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_SYNTHETIC or AsmUtil.NO_FLAG_PACKAGE_PRIVATE,
                EXCEPTION_FIELD_NAME, AsmTypes.JAVA_THROWABLE_TYPE.descriptor, null, null
        )
    }

    private fun generateDoResume() {
        functionCodegen.generateMethod(
                OtherOrigin(element),
                doResumeDescriptor,
                object : FunctionGenerationStrategy.CodegenBased(state) {
                    override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                        StackValue.field(
                                AsmTypes.OBJECT_TYPE, Type.getObjectType(v.thisName), DATA_FIELD_NAME, false,
                                StackValue.LOCAL_0
                        ).store(StackValue.local(1, AsmTypes.OBJECT_TYPE), codegen.v)

                        StackValue.field(
                                AsmTypes.JAVA_THROWABLE_TYPE, Type.getObjectType(v.thisName), EXCEPTION_FIELD_NAME, false,
                                StackValue.LOCAL_0
                        ).store(StackValue.local(2, AsmTypes.JAVA_THROWABLE_TYPE), codegen.v)

                        LABEL_FIELD_STACK_VALUE.store(
                                StackValue.operation(Type.INT_TYPE) {
                                    LABEL_FIELD_STACK_VALUE.put(Type.INT_TYPE, it)
                                    it.iconst(1 shl 31)
                                    it.or(Type.INT_TYPE)
                                },
                                codegen.v
                        )

                        val captureThisType = closure.captureThis?.let(typeMapper::mapType)
                        if (captureThisType != null) {
                            StackValue.field(
                                    captureThisType, Type.getObjectType(v.thisName), AsmUtil.CAPTURED_THIS_FIELD,
                                    false, StackValue.LOCAL_0
                            ).put(captureThisType, codegen.v)
                        }

                        val isInterfaceMethod = DescriptorUtils.isInterface(suspendFunctionJvmView.containingDeclaration)
                        val callableMethod =
                                typeMapper.mapToCallableMethod(
                                        suspendFunctionJvmView,
                                        // Obtain default impls method for interfaces
                                        isInterfaceMethod
                                )

                        for (argumentType in callableMethod.getAsmMethod().argumentTypes.dropLast(1)) {
                            AsmUtil.pushDefaultValueOnStack(argumentType, codegen.v)
                        }

                        codegen.v.load(0, AsmTypes.OBJECT_TYPE)

                        if (suspendFunctionJvmView.isOverridable && !isInterfaceMethod && captureThisType != null) {
                            val owner = captureThisType.internalName
                            val impl = callableMethod.getAsmMethod().getImplForOpenMethod(owner)
                            codegen.v.invokestatic(owner, impl.name, impl.descriptor, false)
                        }
                        else {
                            callableMethod.genInvokeInstruction(codegen.v)
                        }

                        codegen.v.visitInsn(Opcodes.ARETURN)
                    }
                }
        )
    }

    private fun generateGetLabelMethod() {
        val mv = v.newMethod(
                JvmDeclarationOrigin.NO_ORIGIN,
                Opcodes.ACC_SYNTHETIC or Opcodes.ACC_FINAL or AsmUtil.NO_FLAG_PACKAGE_PRIVATE,
                "getLabel",
                Type.getMethodDescriptor(Type.INT_TYPE),
                null,
                null
        )

        mv.visitCode()
        LABEL_FIELD_STACK_VALUE.put(Type.INT_TYPE, InstructionAdapter(mv))
        mv.visitInsn(Opcodes.IRETURN)
        mv.visitEnd()
    }

    private fun generateSetLabelMethod() {
        val mv = v.newMethod(
                JvmDeclarationOrigin.NO_ORIGIN,
                Opcodes.ACC_SYNTHETIC or Opcodes.ACC_FINAL or AsmUtil.NO_FLAG_PACKAGE_PRIVATE,
                "setLabel",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE),
                null,
                null
        )

        mv.visitCode()
        LABEL_FIELD_STACK_VALUE.store(StackValue.local(1, Type.INT_TYPE), InstructionAdapter(mv))
        mv.visitInsn(Opcodes.RETURN)
        mv.visitEnd()
    }

    override fun generateKotlinMetadataAnnotation() {
        writeKotlinMetadata(v, state, KotlinClassHeader.Kind.SYNTHETIC_CLASS, 0) {
            // Do not write method metadata for raw coroutine state machines
        }
    }

    companion object {
        private val LABEL_FIELD_STACK_VALUE =
                StackValue.field(
                        FieldInfo.createForHiddenField(COROUTINE_IMPL_ASM_TYPE, Type.INT_TYPE, COROUTINE_LABEL_FIELD_NAME),
                        StackValue.LOCAL_0
                )

        fun create(
                cv: ClassBuilder,
                expressionCodegen: ExpressionCodegen,
                originalSuspendDescriptor: FunctionDescriptor,
                declaration: KtFunction
        ): CoroutineCodegenForNamedFunction {
            val bindingContext = expressionCodegen.state.bindingContext
            val closure =
                    bindingContext[
                            CodegenBinding.CLOSURE,
                            bindingContext[CodegenBinding.CLASS_FOR_CALLABLE, originalSuspendDescriptor]
                    ].sure { "There must be a closure defined for $originalSuspendDescriptor" }

            val suspendFunctionView =
                    bindingContext[
                            CodegenBinding.SUSPEND_FUNCTION_TO_JVM_VIEW, originalSuspendDescriptor
                    ].sure { "There must be a jvm view defined for $originalSuspendDescriptor" }

            if (suspendFunctionView.dispatchReceiverParameter != null) {
                closure.setCaptureThis()
            }

            return CoroutineCodegenForNamedFunction(
                    expressionCodegen, declaration,
                    expressionCodegen.context.intoClosure(
                            originalSuspendDescriptor, expressionCodegen, expressionCodegen.state.typeMapper
                    ),
                    cv,
                    originalSuspendDescriptor
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
