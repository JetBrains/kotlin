/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.builtins.isSuspendFunctionTypeOrSubtype
import org.jetbrains.kotlin.cfg.index
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.binding.CalculatedClosure
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.binding.CodegenBinding.CAPTURES_CROSSINLINE_LAMBDA
import org.jetbrains.kotlin.codegen.binding.CodegenBinding.CLOSURE
import org.jetbrains.kotlin.codegen.context.ClosureContext
import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings.METHOD_FOR_FUNCTION
import org.jetbrains.kotlin.codegen.serialization.JvmSerializerExtension
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.isReleaseCoroutines
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.sure
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.MethodNode

abstract class AbstractCoroutineCodegen(
    outerExpressionCodegen: ExpressionCodegen,
    element: KtElement,
    closureContext: ClosureContext,
    classBuilder: ClassBuilder,
    private val userDataForDoResume: Map<out CallableDescriptor.UserDataKey<*>, *>? = null
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
                SUSPEND_CALL_RESULT_NAME to classDescriptor.module.getResult(classDescriptor.builtIns.anyType)
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
                DescriptorVisibilities.PUBLIC,
                userDataForDoResume
            )
        }

    private fun FunctionDescriptor.createValueParameterForDoResume(name: Name, type: KotlinType, index: Int) =
        ValueParameterDescriptorImpl(
            this, null, index, Annotations.EMPTY, name,
            type,
            declaresDefaultValue = false, isCrossinline = false,
            isNoinline = false,
            varargElementType = null, source = SourceElement.NO_SOURCE
        )

    override fun generateConstructor(): Method {
        val args = calculateConstructorParameters(typeMapper, languageVersionSettings, closure, asmType)
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
                iv.iconst(if (passArityToSuperClass) funDescriptor.arity else 0)
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

    protected abstract val passArityToSuperClass: Boolean
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

    private val endLabel = Label()

    private val varsCountByType = hashMapOf<Type, Int>()

    private val fieldsForParameters: Map<ParameterDescriptor, FieldInfo> = createFieldsForParameters()

    private fun createFieldsForParameters(): Map<ParameterDescriptor, FieldInfo> {
        val result = hashMapOf<ParameterDescriptor, FieldInfo>()
        for (parameter in allFunctionParameters()) {
            if (parameter.isUnused()) continue
            val type = state.typeMapper.mapType(parameter.type)
            val normalizedType = type.normalize()
            val index = varsCountByType[normalizedType]?.plus(1) ?: 0
            varsCountByType[normalizedType] = index
            result[parameter] = createHiddenFieldInfo(parameter.type, "${normalizedType.descriptor[0]}$$index")
        }
        return result
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
            DescriptorVisibilities.PUBLIC
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
            val fieldInfo = fieldsForParameters[parameter] ?: continue
            v.newField(
                OtherOrigin(parameter),
                Opcodes.ACC_PRIVATE + Opcodes.ACC_SYNTHETIC,
                fieldInfo.fieldName,
                fieldInfo.fieldType.descriptor, null, null
            )
        }

        generateResumeImpl()
    }

    private fun ParameterDescriptor.isUnused(): Boolean =
        originalSuspendFunctionDescriptor is AnonymousFunctionDescriptor &&
                bindingContext[BindingContext.SUSPEND_LAMBDA_PARAMETER_USED, originalSuspendFunctionDescriptor to index()] != true

    private val generateErasedCreate: Boolean = allFunctionParameters().size <= 1

    private val doNotGenerateInvokeBridge: Boolean = !originalSuspendFunctionDescriptor.isLocalSuspendFunctionNotSuspendLambda()

    override fun generateBody() {
        super.generateBody()

        if (doNotGenerateInvokeBridge) {
            v.serializationBindings.put(
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
                                                   codegen.v.generateInvokeMethod(signature, funDescriptor)
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
            OtherOrigin(element, funDescriptor), DescriptorAsmUtil.getVisibilityAccessFlag(untypedDescriptor) or Opcodes.ACC_FINAL,
            untypedAsmMethod.name, untypedAsmMethod.descriptor, null, ArrayUtil.EMPTY_STRING_ARRAY
        )
        mv.visitCode()
        with(InstructionAdapter(mv)) {
            generateInvokeMethod(jvmMethodSignature, untypedDescriptor)
        }

        FunctionCodegen.endVisit(mv, "invoke", element)
    }

    private fun InstructionAdapter.generateInvokeMethod(signature: JvmMethodSignature, descriptor: FunctionDescriptor) {
        // this
        load(0, AsmTypes.OBJECT_TYPE)
        val parameterTypes = signature.valueParameters.map { it.asmType }
        val createArgumentTypes =
            if (generateErasedCreate || doNotGenerateInvokeBridge) typeMapper.mapAsmMethod(createCoroutineDescriptor).argumentTypes.asList()
            else parameterTypes
        // invoke is not big arity, but create is. Pass an array to create.
        if (parameterTypes.size == 22 && createArgumentTypes.size == 1) {
            iconst(22)
            newarray(AsmTypes.OBJECT_TYPE)
            // 0 - this
            // 1..22 - parameters
            // 23 - first empty slot
            val arraySlot = 23
            store(arraySlot, AsmTypes.OBJECT_TYPE)
            for ((varIndex, type) in parameterTypes.withVariableIndices()) {
                load(arraySlot, AsmTypes.OBJECT_TYPE)
                iconst(varIndex)
                load(varIndex + 1, type)
                StackValue.coerce(type, AsmTypes.OBJECT_TYPE, this)
                astore(AsmTypes.OBJECT_TYPE)
            }
            load(arraySlot, AsmTypes.OBJECT_TYPE)
        } else {
            var index = 0
            val fromKotlinTypes =
                if (!generateErasedCreate && doNotGenerateInvokeBridge) funDescriptor.allValueParameterTypes()
                else funDescriptor.allValueParameterTypes().map { funDescriptor.module.builtIns.nullableAnyType }
            val toKotlinTypes =
                if (!generateErasedCreate && doNotGenerateInvokeBridge) createCoroutineDescriptor.allValueParameterTypes()
                else descriptor.allValueParameterTypes()
            parameterTypes.withVariableIndices().forEach { (varIndex, type) ->
                load(varIndex + 1, type)
                StackValue.coerce(type, fromKotlinTypes[index], createArgumentTypes[index], toKotlinTypes[index], this)
                index++
            }
        }

        // this.create(..)
        invokevirtual(
            v.thisName,
            typeMapper.mapFunctionName(createCoroutineDescriptor, null),
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
        val isBigArity = JvmCodegenUtil.isDeclarationOfBigArityCreateCoroutineMethod(createCoroutineDescriptor)

        with(codegen.v) {
            anew(owner)
            dup()

            // pass captured closure to constructor
            val constructorParameters = calculateConstructorParameters(typeMapper, languageVersionSettings, closure, owner)
            for (parameter in constructorParameters) {
                StackValue.field(parameter, thisInstance).put(parameter.fieldType, parameter.fieldKotlinType, this)
            }

            // load resultContinuation
            if (isBigArity) {
                load(1, AsmTypes.OBJECT_TYPE)
                iconst(allFunctionParameters().size)
                aload(AsmTypes.OBJECT_TYPE)
            } else {
                if (generateErasedCreate) {
                    load(allFunctionParameters().size + 1, AsmTypes.OBJECT_TYPE)
                } else {
                    load(allFunctionParameters().map { typeMapper.mapType(it.type).size }.sum() + 1, AsmTypes.OBJECT_TYPE)
                }
            }

            invokespecial(owner.internalName, constructorToUseFromInvoke.name, constructorToUseFromInvoke.descriptor, false)

            val cloneIndex = codegen.frameMap.enterTemp(AsmTypes.OBJECT_TYPE)
            store(cloneIndex, AsmTypes.OBJECT_TYPE)

            // Pass lambda parameters to 'invoke' call on newly constructed object
            var index = 1
            for (parameter in allFunctionParameters()) {
                val fieldInfoForCoroutineLambdaParameter = fieldsForParameters[parameter]
                if (fieldInfoForCoroutineLambdaParameter != null) {
                    if (isBigArity) {
                        load(cloneIndex, fieldInfoForCoroutineLambdaParameter.ownerType)
                        load(1, AsmTypes.OBJECT_TYPE)
                        iconst(index - 1)
                        aload(AsmTypes.OBJECT_TYPE)
                        StackValue.coerce(
                            AsmTypes.OBJECT_TYPE, builtIns.nullableAnyType,
                            fieldInfoForCoroutineLambdaParameter.fieldType, fieldInfoForCoroutineLambdaParameter.fieldKotlinType,
                            this
                        )
                        putfield(
                            fieldInfoForCoroutineLambdaParameter.ownerInternalName,
                            fieldInfoForCoroutineLambdaParameter.fieldName,
                            fieldInfoForCoroutineLambdaParameter.fieldType.descriptor
                        )
                    } else {
                        if (generateErasedCreate) {
                            if (parameter.type.isInlineClassType()) {
                                load(cloneIndex, fieldInfoForCoroutineLambdaParameter.ownerType)
                                load(index, AsmTypes.OBJECT_TYPE)
                                StackValue.unboxInlineClass(AsmTypes.OBJECT_TYPE, parameter.type, this)
                                putfield(
                                    fieldInfoForCoroutineLambdaParameter.ownerInternalName,
                                    fieldInfoForCoroutineLambdaParameter.fieldName,
                                    fieldInfoForCoroutineLambdaParameter.fieldType.descriptor
                                )
                                continue
                            } else {
                                load(index, AsmTypes.OBJECT_TYPE)
                                StackValue.coerce(
                                    AsmTypes.OBJECT_TYPE, builtIns.nullableAnyType,
                                    fieldInfoForCoroutineLambdaParameter.fieldType, fieldInfoForCoroutineLambdaParameter.fieldKotlinType,
                                    this
                                )
                            }
                        } else {
                            load(index, fieldInfoForCoroutineLambdaParameter.fieldType)
                        }
                        DescriptorAsmUtil.genAssignInstanceFieldFromParam(
                            fieldInfoForCoroutineLambdaParameter,
                            index,
                            this,
                            cloneIndex,
                            generateErasedCreate
                        )
                    }
                }
                index += if (isBigArity || generateErasedCreate) 1 else state.typeMapper.mapType(parameter.type).size
            }

            load(cloneIndex, AsmTypes.OBJECT_TYPE)
            areturn(AsmTypes.OBJECT_TYPE)
        }
    }

    private fun ExpressionCodegen.initializeCoroutineParameters() {
        for (parameter in allFunctionParameters()) {
            val fieldForParameter = fieldsForParameters[parameter] ?: continue
            val fieldStackValue = StackValue.field(fieldForParameter, generateThisOrOuter(context.thisDescriptor, false))

            val originalType = typeMapper.mapType(parameter.type)
            // If a parameter has reference type, it has prefix L$,
            // however, when the type is primitive, its prefix is ${type.descriptor}$.
            // In other words, it the type is Boolean, the prefix is Z$.
            // This is different from spilled variables, where all int-like primitives have prefix I$.
            // This is not a problem, since we do not clean spilled primitives up
            // and we do not coerce Int to Boolean, which takes quite a bit of bytecode (see coerceInt).
            val normalizedType = originalType.normalize()
            fieldStackValue.put(normalizedType, v)

            val newIndex = myFrameMap.enter(parameter, originalType)
            StackValue.coerce(normalizedType, originalType, v)
            v.store(newIndex, originalType)

            val name =
                if (parameter is ReceiverParameterDescriptor)
                    DescriptorAsmUtil.getNameForReceiverParameter(originalSuspendFunctionDescriptor, bindingContext, languageVersionSettings)
                else
                    (getNameForDestructuredParameterOrNull(parameter as ValueParameterDescriptor) ?: parameter.name.asString())
            val label = Label()
            v.mark(label)
            v.visitLocalVariable(name, originalType.descriptor, null, label, endLabel, newIndex)
        }

        initializeVariablesForDestructuredLambdaParameters(
            this,
            originalSuspendFunctionDescriptor.valueParameters.filter { !it.isUnused() },
            endLabel
        )
    }

    private fun allFunctionParameters(): List<ParameterDescriptor> =
        originalSuspendFunctionDescriptor.extensionReceiverParameter.let(::listOfNotNull) +
                originalSuspendFunctionDescriptor.valueParameters

    private fun createHiddenFieldInfo(type: KotlinType, name: String) =
        FieldInfo.createForHiddenField(
            typeMapper.mapClass(closureContext.thisDescriptor),
            typeMapper.mapType(type).normalize(),
            type,
            name
        )

    private fun generateResumeImpl() {
        functionCodegen.generateMethod(
            OtherOrigin(element),
            methodToImplement,
            object : FunctionGenerationStrategy.FunctionDefault(state, element as KtDeclarationWithBody) {

                override fun wrapMethodVisitor(mv: MethodVisitor, access: Int, name: String, desc: String): MethodVisitor {
                    val stateMachineBuilder = CoroutineTransformerMethodVisitor(
                        mv, access, name, desc, null, null,
                        obtainClassBuilderForCoroutineState = { v },
                        reportSuspensionPointInsideMonitor = { reportSuspensionPointInsideMonitor(element, state, it) },
                        lineNumber = CodegenUtil.getLineNumberForElement(element, false) ?: 0,
                        sourceFile = element.containingKtFile.name,
                        shouldPreserveClassInitialization = constructorCallNormalizationMode.shouldPreserveClassInitialization,
                        containingClassInternalName = v.thisName,
                        isForNamedFunction = false,
                        languageVersionSettings = languageVersionSettings,
                        disableTailCallOptimizationForFunctionReturningUnit = false,
                        useOldSpilledVarTypeAnalysis = state.configuration.getBoolean(JVMConfigurationKeys.USE_OLD_SPILLED_VAR_TYPE_ANALYSIS),
                        initialVarsCountByType = varsCountByType
                    )
                    val maybeWithForInline = if (forInline)
                        SuspendForInlineCopyingMethodVisitor(stateMachineBuilder, access, name, desc, functionCodegen::newMethod)
                    else
                        stateMachineBuilder
                    return AddEndLabelMethodVisitor(maybeWithForInline, access, name, desc, endLabel)
                }

                override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                    if (element is KtFunctionLiteral) {
                        recordCallLabelForLambdaArgument(element, state.bindingTrace)
                    }
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
                expressionCodegen.bindingContext[CAPTURES_CROSSINLINE_LAMBDA, originalSuspendLambdaDescriptor] == true
            )
        }
    }
}

fun isCapturedSuspendLambda(closure: CalculatedClosure, name: String, bindingContext: BindingContext): Boolean {
    for ((param, value) in closure.captureVariables) {
        if (param !is ValueParameterDescriptor) continue
        if (value.fieldName != name) continue
        return param.type.isSuspendFunctionTypeOrSubtype
    }
    val classDescriptor = closure.capturedOuterClassDescriptor ?: return false
    return isCapturedSuspendLambda(classDescriptor, name, bindingContext)
}

fun isCapturedSuspendLambda(classDescriptor: ClassDescriptor, name: String, bindingContext: BindingContext): Boolean {
    val closure = bindingContext[CLOSURE, classDescriptor] ?: return false
    return isCapturedSuspendLambda(closure, name, bindingContext)
}

private class AddEndLabelMethodVisitor(
    delegate: MethodVisitor,
    access: Int,
    name: String,
    desc: String,
    private val endLabel: Label
) : TransformationMethodVisitor(delegate, access, name, desc, null, null) {
    override fun performTransformations(methodNode: MethodNode) {
        methodNode.instructions.add(
            withInstructionAdapter {
                mark(endLabel)
            }
        )
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

    private val inlineClassToBoxInInvokeSuspend: KotlinType? =
        originalSuspendFunctionDescriptor.originalReturnTypeOfSuspendFunctionReturningUnboxedInlineClass(state.typeMapper)

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
            languageVersionSettings.dataFieldName(), AsmTypes.OBJECT_TYPE.descriptor, null, null
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
                        AsmTypes.OBJECT_TYPE, Type.getObjectType(v.thisName), languageVersionSettings.dataFieldName(), false,
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

                    val captureThis = closure.capturedOuterClassDescriptor
                    val captureThisType = captureThis?.let(typeMapper::mapType)
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

                    if (inlineClassToBoxInInvokeSuspend != null) {
                        with(codegen.v) {
                            // We need to box the returned inline class in resume path.
                            // But first, check for COROUTINE_SUSPENDED, since the function can return it
                            generateCoroutineSuspendedCheck(languageVersionSettings)
                            // Now we box the inline class
                            StackValue.coerce(AsmTypes.OBJECT_TYPE, typeMapper.mapType(inlineClassToBoxInInvokeSuspend), this)
                            StackValue.boxInlineClass(inlineClassToBoxInInvokeSuspend, this)
                        }
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
            val functionProto =
                serializer.functionProto(
                    createFreeFakeLambdaDescriptor(suspendFunctionJvmView, state.typeApproximator)
                )?.build() ?: return@writeKotlinMetadata
            DescriptorAsmUtil.writeAnnotationData(av, serializer, functionProto)
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
            val closure = bindingContext[CLOSURE, bindingContext[CodegenBinding.CLASS_FOR_CALLABLE, originalSuspendDescriptor]]
                .sure { "There must be a closure defined for $originalSuspendDescriptor" }

            val suspendFunctionView = bindingContext[CodegenBinding.SUSPEND_FUNCTION_TO_JVM_VIEW, originalSuspendDescriptor]
                .sure { "There must be a jvm view defined for $originalSuspendDescriptor" }

            if (suspendFunctionView.dispatchReceiverParameter != null) {
                closure.setNeedsCaptureOuterClass()
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

private object FailingFunctionGenerationStrategy : FunctionGenerationStrategy() {
    override fun skipNotNullAssertionsForParameters(): Boolean {
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

fun reportSuspensionPointInsideMonitor(element: KtElement, state: GenerationState, stackTraceElement: String) {
    state.diagnostics.report(ErrorsJvm.SUSPENSION_POINT_INSIDE_MONITOR.on(element, stackTraceElement))
}

private fun FunctionDescriptor.allValueParameterTypes(): List<KotlinType> =
    (listOfNotNull(extensionReceiverParameter?.type)) + valueParameters.map { it.type }