/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.binding.CodegenBinding.CAPTURES_CROSSINLINE_SUSPEND_LAMBDA
import org.jetbrains.kotlin.codegen.context.ClosureContext
import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings.METHOD_FOR_FUNCTION
import org.jetbrains.kotlin.codegen.serialization.JvmSerializerExtension
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.isReleaseCoroutines
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.serialization.DescriptorSerializer
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
    private val userDataForDoResume: Map<out FunctionDescriptor.UserDataKey<*>, *>? = null
) : ClosureCodegen(
    outerExpressionCodegen.state,
    element, null, closureContext, null,
    FailingFunctionGenerationStrategy,
    outerExpressionCodegen.parentCodegen, classBuilder
) {
    protected val classDescriptor = closureContext.contextDescriptor
    protected val languageVersionSettings = outerExpressionCodegen.state.languageVersionSettings

    protected val methodToImplement =
        if (languageVersionSettings.isReleaseCoroutines())
            createImplMethod(
                INVOKE_SUSPEND_METHOD_NAME,
                "result" to classDescriptor.module.getSuccessOrFailure(classDescriptor.builtIns.anyType)
            )
        else
            createImplMethod(
                DO_RESUME_METHOD_NAME,
                "data" to classDescriptor.builtIns.nullableAnyType,
                "throwable" to classDescriptor.builtIns.throwable.defaultType.makeNullable()
            )

    private fun createImplMethod(name: String, vararg parameters: Pair<String, KotlinType>) =
        SimpleFunctionDescriptorImpl.create(
            classDescriptor, Annotations.EMPTY, Name.identifier(name), CallableMemberDescriptor.Kind.DECLARATION,
            funDescriptor.source
        ).apply {
            initialize(
                null,
                classDescriptor.thisAsReceiverParameter,
                emptyList(),
                parameters.withIndex().map { (index, nameAndType) ->
                    createValueParameterForDoResume(Name.identifier(nameAndType.first), nameAndType.second, index)
                },
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
        val argTypes = args.map { it.fieldType }.plus(languageVersionSettings.continuationAsmType()).toTypedArray()

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
            val hasArityParameter = !languageVersionSettings.isReleaseCoroutines() || passArityToSuperClass
            if (hasArityParameter) {
                iv.iconst(if (passArityToSuperClass) calculateArity() else 0)
            }

            iv.load(argTypes.map { it.size }.sum(), AsmTypes.OBJECT_TYPE)

            val parameters =
                if (hasArityParameter)
                    listOf(Type.INT_TYPE, languageVersionSettings.continuationAsmType())
                else
                    listOf(languageVersionSettings.continuationAsmType())

            val superClassConstructorDescriptor = Type.getMethodDescriptor(
                Type.VOID_TYPE,
                *parameters.toTypedArray()
            )
            iv.invokespecial(superClassAsmType.internalName, "<init>", superClassConstructorDescriptor, false)

            iv.visitInsn(Opcodes.RETURN)

            FunctionCodegen.endVisit(iv, "constructor", element)
        }

        if (languageVersionSettings.isReleaseCoroutines()) {
            v.newField(JvmDeclarationOrigin.NO_ORIGIN, AsmUtil.NO_FLAG_PACKAGE_PRIVATE, "label", "I", null, null)
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
    private val originalSuspendFunctionDescriptor: FunctionDescriptor,
    private val forInline: Boolean
) : AbstractCoroutineCodegen(
    outerExpressionCodegen, element, closureContext, classBuilder,
    userDataForDoResume = mapOf(INITIAL_SUSPEND_DESCRIPTOR_FOR_DO_RESUME to originalSuspendFunctionDescriptor)
) {
    private val builtIns = funDescriptor.builtIns

    private val constructorCallNormalizationMode = outerExpressionCodegen.state.constructorCallNormalizationMode

    private val erasedInvokeFunction by lazy {
        getErasedInvokeFunction(funDescriptor).createCustomCopy { setModality(Modality.FINAL) }
    }

    private lateinit var constructorToUseFromInvoke: Method

    private val createCoroutineDescriptor by lazy {
        if (generateErasedCreate) getErasedCreateFunction() else getCreateFunction()
    }

    private fun getCreateFunction(): SimpleFunctionDescriptor = SimpleFunctionDescriptorImpl.create(
        funDescriptor.containingDeclaration,
        Annotations.EMPTY,
        Name.identifier(SUSPEND_FUNCTION_CREATE_METHOD_NAME),
        funDescriptor.kind,
        funDescriptor.source
    ).also {
        it.initialize(
            funDescriptor.extensionReceiverParameter?.copy(it),
            funDescriptor.dispatchReceiverParameter,
            funDescriptor.typeParameters,
            funDescriptor.valueParameters,
            funDescriptor.module.getContinuationOfTypeOrAny(
                builtIns.unitType,
                state.languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines)
            ),
            funDescriptor.modality,
            Visibilities.PUBLIC
        )
    }

    private fun getErasedCreateFunction(): SimpleFunctionDescriptor {
        val typedCreate = getCreateFunction()
        assert(generateErasedCreate) { "cannot create erased create function: $typedCreate" }
        val argumentsNum = typeMapper.mapSignatureSkipGeneric(typedCreate).asmMethod.argumentTypes.size
        assert(argumentsNum == 1 || argumentsNum == 2) {
            "too many arguments of create to have an erased signature: $argumentsNum: $typedCreate"
        }
        return typedCreate.module.resolveClassByFqName(
            languageVersionSettings.coroutinesJvmInternalPackageFqName().child(
                if (languageVersionSettings.isReleaseCoroutines())
                    Name.identifier("BaseContinuationImpl")
                else
                    Name.identifier("CoroutineImpl")
            ),
            NoLookupLocation.FROM_BACKEND
        ).sure { "BaseContinuationImpl or CoroutineImpl is not found" }.defaultType.memberScope
            .getContributedFunctions(typedCreate.name, NoLookupLocation.FROM_BACKEND)
            .find { it.valueParameters.size == argumentsNum }
            .sure { "erased parent of $typedCreate is not found" }
            .createCustomCopy { setModality(Modality.FINAL) }
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

        generateResumeImpl()
    }

    private val generateErasedCreate: Boolean = allFunctionParameters().size <= 1

    private val doNotGenerateInvokeBridge: Boolean = !originalSuspendFunctionDescriptor.isLocalSuspendFunctionNotSuspendLambda()

    override fun generateBody() {
        super.generateBody()

        if (doNotGenerateInvokeBridge) {
            v.serializationBindings.put<FunctionDescriptor, Method>(
                METHOD_FOR_FUNCTION,
                originalSuspendFunctionDescriptor,
                typeMapper.mapAsmMethod(erasedInvokeFunction)
            )
        }

        // create() = ...
        functionCodegen.generateMethod(JvmDeclarationOrigin.NO_ORIGIN, createCoroutineDescriptor,
                                       object : FunctionGenerationStrategy.CodegenBased(state) {
                                           override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                                               generateCreateCoroutineMethod(codegen)
                                           }
                                       })

        if (doNotGenerateInvokeBridge) {
            generateUntypedInvokeMethod()
        } else {
            // invoke(..) = create(..).resume(Unit)
            functionCodegen.generateMethod(JvmDeclarationOrigin.NO_ORIGIN, funDescriptor,
                                           object : FunctionGenerationStrategy.CodegenBased(state) {
                                               override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                                                   codegen.v.generateInvokeMethod(signature)
                                               }
                                           })
        }
    }

    override fun generateBridges() {
        if (!doNotGenerateInvokeBridge) {
            super.generateBridges()
        }
    }

    private fun generateUntypedInvokeMethod() {
        val untypedDescriptor = getErasedInvokeFunction(funDescriptor)
        val untypedAsmMethod = typeMapper.mapAsmMethod(untypedDescriptor)
        val jvmMethodSignature = typeMapper.mapSignatureSkipGeneric(untypedDescriptor)
        val mv = v.newMethod(
            OtherOrigin(element, funDescriptor), AsmUtil.getVisibilityAccessFlag(untypedDescriptor) or Opcodes.ACC_FINAL,
            untypedAsmMethod.name, untypedAsmMethod.descriptor, null, ArrayUtil.EMPTY_STRING_ARRAY
        )
        mv.visitCode()
        with(InstructionAdapter(mv)) {
            generateInvokeMethod(jvmMethodSignature)
        }

        FunctionCodegen.endVisit(mv, "invoke", element)
    }

    private fun InstructionAdapter.generateInvokeMethod(signature: JvmMethodSignature) {
        // this
        load(0, AsmTypes.OBJECT_TYPE)
        val parameterTypes = signature.valueParameters.map { it.asmType }
        val createArgumentTypes =
            if (generateErasedCreate || doNotGenerateInvokeBridge) typeMapper.mapAsmMethod(createCoroutineDescriptor).argumentTypes.asList()
            else parameterTypes
        var index = 0
        parameterTypes.withVariableIndices().forEach { (varIndex, type) ->
            load(varIndex + 1, type)
            StackValue.coerce(type, createArgumentTypes[index++], this)
        }

        // this.create(..)
        invokevirtual(
            v.thisName,
            createCoroutineDescriptor.name.identifier,
            Type.getMethodDescriptor(
                languageVersionSettings.continuationAsmType(),
                *createArgumentTypes.toTypedArray()
            ),
            false
        )
        checkcast(Type.getObjectType(v.thisName))

        // .doResume(Unit)
        if (languageVersionSettings.isReleaseCoroutines()) {
            invokeInvokeSuspendWithUnit(v.thisName)
        } else {
            invokeDoResumeWithUnit(v.thisName)
        }
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
            if (generateErasedCreate) {
                load(allFunctionParameters().size + 1, AsmTypes.OBJECT_TYPE)
            } else {
                load(allFunctionParameters().map { typeMapper.mapType(it.type).size }.sum() + 1, AsmTypes.OBJECT_TYPE)
            }

            invokespecial(owner.internalName, constructorToUseFromInvoke.name, constructorToUseFromInvoke.descriptor, false)

            val cloneIndex = codegen.frameMap.enterTemp(AsmTypes.OBJECT_TYPE)
            store(cloneIndex, AsmTypes.OBJECT_TYPE)

            // Pass lambda parameters to 'invoke' call on newly constructed object
            var index = 1
            for (parameter in allFunctionParameters()) {
                val fieldInfoForCoroutineLambdaParameter = parameter.getFieldInfoForCoroutineLambdaParameter()
                if (generateErasedCreate) {
                    load(index, AsmTypes.OBJECT_TYPE)
                    StackValue.coerce(AsmTypes.OBJECT_TYPE, fieldInfoForCoroutineLambdaParameter.fieldType, this)
                } else {
                    load(index, fieldInfoForCoroutineLambdaParameter.fieldType)
                }
                AsmUtil.genAssignInstanceFieldFromParam(
                    fieldInfoForCoroutineLambdaParameter,
                    index,
                    this,
                    cloneIndex,
                    generateErasedCreate
                )
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
                originalSuspendFunctionDescriptor.valueParameters

    private fun ParameterDescriptor.getFieldInfoForCoroutineLambdaParameter() =
        createHiddenFieldInfo(type, COROUTINE_LAMBDA_PARAMETER_PREFIX + (this.safeAs<ValueParameterDescriptor>()?.index ?: ""))

    private fun createHiddenFieldInfo(type: KotlinType, name: String) =
        FieldInfo.createForHiddenField(
            typeMapper.mapClass(closureContext.thisDescriptor),
            typeMapper.mapType(type),
            name
        )

    private fun generateResumeImpl() {
        functionCodegen.generateMethod(
            OtherOrigin(element),
            methodToImplement,
            object : FunctionGenerationStrategy.FunctionDefault(state, element as KtDeclarationWithBody) {

                override fun wrapMethodVisitor(mv: MethodVisitor, access: Int, name: String, desc: String): MethodVisitor {
                    if (forInline) return super.wrapMethodVisitor(mv, access, name, desc)
                    return CoroutineTransformerMethodVisitor(
                        mv, access, name, desc, null, null,
                        obtainClassBuilderForCoroutineState = { v },
                        lineNumber = CodegenUtil.getLineNumberForElement(element, false) ?: 0,
                        shouldPreserveClassInitialization = constructorCallNormalizationMode.shouldPreserveClassInitialization,
                        containingClassInternalName = v.thisName,
                        isForNamedFunction = false,
                        languageVersionSettings = languageVersionSettings
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
            if (!originalSuspendLambdaDescriptor.isSuspendLambdaOrLocalFunction() || declaration is KtCallableReferenceExpression) return null

            return CoroutineCodegenForLambda(
                expressionCodegen,
                declaration,
                expressionCodegen.context.intoCoroutineClosure(
                    getOrCreateJvmSuspendFunctionView(
                        originalSuspendLambdaDescriptor,
                        expressionCodegen.state
                    ),
                    originalSuspendLambdaDescriptor, expressionCodegen, expressionCodegen.state.typeMapper
                ),
                classBuilder,
                originalSuspendLambdaDescriptor,
                // Local suspend lambdas, which call crossinline suspend parameters of containing functions must be generated after inlining
                expressionCodegen.bindingContext[CAPTURES_CROSSINLINE_SUSPEND_LAMBDA, originalSuspendLambdaDescriptor] == true
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
    private val labelFieldStackValue by lazy {
        StackValue.field(
            FieldInfo.createForHiddenField(
                computeLabelOwner(languageVersionSettings, v.thisName),
                Type.INT_TYPE,
                COROUTINE_LABEL_FIELD_NAME
            ),
            StackValue.LOCAL_0
        )
    }


    private val suspendFunctionJvmView =
        bindingContext[CodegenBinding.SUSPEND_FUNCTION_TO_JVM_VIEW, originalSuspendFunctionDescriptor]!!

    override val passArityToSuperClass get() = false

    override fun generateBridges() {
        // Do not generate any closure bridges
    }

    override fun generateClosureBody() {
        generateResumeImpl()

        if (!languageVersionSettings.isReleaseCoroutines()) {
            generateGetLabelMethod()
            generateSetLabelMethod()
        }

        v.newField(
            JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_SYNTHETIC or AsmUtil.NO_FLAG_PACKAGE_PRIVATE,
            DATA_FIELD_NAME, AsmTypes.OBJECT_TYPE.descriptor, null, null
        )

        if (!languageVersionSettings.isReleaseCoroutines()) {
            v.newField(
                JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_SYNTHETIC or AsmUtil.NO_FLAG_PACKAGE_PRIVATE,
                EXCEPTION_FIELD_NAME, AsmTypes.JAVA_THROWABLE_TYPE.descriptor, null, null
            )
        }
    }

    private fun generateResumeImpl() {
        functionCodegen.generateMethod(
            OtherOrigin(element),
            methodToImplement,
            object : FunctionGenerationStrategy.CodegenBased(state) {
                override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                    StackValue.field(
                        AsmTypes.OBJECT_TYPE, Type.getObjectType(v.thisName), DATA_FIELD_NAME, false,
                        StackValue.LOCAL_0
                    ).store(StackValue.local(1, AsmTypes.OBJECT_TYPE), codegen.v)

                    if (!languageVersionSettings.isReleaseCoroutines()) {
                        StackValue.field(
                            AsmTypes.JAVA_THROWABLE_TYPE, Type.getObjectType(v.thisName), EXCEPTION_FIELD_NAME, false,
                            StackValue.LOCAL_0
                        ).store(StackValue.local(2, AsmTypes.JAVA_THROWABLE_TYPE), codegen.v)
                    }

                    labelFieldStackValue.store(
                        StackValue.operation(Type.INT_TYPE) {
                            labelFieldStackValue.put(Type.INT_TYPE, it)
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
                    } else {
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
        labelFieldStackValue.put(Type.INT_TYPE, InstructionAdapter(mv))
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
        labelFieldStackValue.store(StackValue.local(1, Type.INT_TYPE), InstructionAdapter(mv))
        mv.visitInsn(Opcodes.RETURN)
        mv.visitEnd()
    }

    override fun generateKotlinMetadataAnnotation() {
        writeKotlinMetadata(v, state, KotlinClassHeader.Kind.SYNTHETIC_CLASS, 0) { av ->
            val serializer = DescriptorSerializer.createForLambda(JvmSerializerExtension(v.serializationBindings, state))
            val functionProto = serializer.functionProto(createFreeFakeLambdaDescriptor(suspendFunctionJvmView)).build()
            AsmUtil.writeAnnotationData(av, serializer, functionProto)
        }
    }

    companion object {
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
    override fun skipNotNullAssertionsForParameters(): kotlin.Boolean {
        error("This functions must not be called")
    }

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
