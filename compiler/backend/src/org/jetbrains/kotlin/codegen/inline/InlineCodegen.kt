/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import com.intellij.psi.PsiElement
import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.backend.common.isBuiltInIntercepted
import org.jetbrains.kotlin.builtins.BuiltInsPackageFragment
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.AsmUtil.getMethodAsmFlags
import org.jetbrains.kotlin.codegen.AsmUtil.isPrimitive
import org.jetbrains.kotlin.codegen.context.ClosureContext
import org.jetbrains.kotlin.codegen.coroutines.createMethodNodeForCoroutineContext
import org.jetbrains.kotlin.codegen.coroutines.createMethodNodeForIntercepted
import org.jetbrains.kotlin.codegen.coroutines.createMethodNodeForSuspendCoroutineUninterceptedOrReturn
import org.jetbrains.kotlin.codegen.coroutines.isBuiltInSuspendCoroutineUninterceptedOrReturnInJvm
import org.jetbrains.kotlin.codegen.intrinsics.bytecode
import org.jetbrains.kotlin.codegen.intrinsics.classId
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.isReleaseCoroutines
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.isInlineOnly
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.ImportedFromObjectCallableDescriptor
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.calls.checkers.isBuiltInCoroutineContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.inline.InlineUtil.isInlinableParameterExpression
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.isFunctionLiteral
import org.jetbrains.kotlin.types.expressions.LabelResolver
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.*
import java.util.*
import kotlin.collections.HashSet

abstract class InlineCodegen<out T : BaseExpressionCodegen>(
    protected val codegen: T,
    protected val state: GenerationState,
    function: FunctionDescriptor,
    private val typeParameterMappings: TypeParameterMappings,
    protected val sourceCompiler: SourceCompilerForInline
) {
    init {
        assert(InlineUtil.isInline(function) || InlineUtil.isArrayConstructorWithLambda(function)) {
            "InlineCodegen can inline only inline functions and array constructors: " + function
        }
    }

    // TODO: implement AS_FUNCTION inline strategy
    private val asFunctionInline = false

    protected val typeMapper = state.typeMapper

    private val initialFrameSize = codegen.frameMap.currentSize

    private val reifiedTypeInliner = ReifiedTypeInliner(typeParameterMappings, state.languageVersionSettings.isReleaseCoroutines())

    protected val functionDescriptor: FunctionDescriptor =
        if (InlineUtil.isArrayConstructorWithLambda(function))
            FictitiousArrayConstructor.create(function as ConstructorDescriptor)
        else
            function.original

    protected val jvmSignature: JvmMethodGenericSignature

    private val isSameModule: Boolean

    protected val invocationParamBuilder = ParametersBuilder.newBuilder()

    protected val expressionMap = linkedMapOf<Int, LambdaInfo>()

    var activeLambda: LambdaInfo? = null
        protected set

    private val defaultSourceMapper = sourceCompiler.lazySourceMapper

    protected var delayedHiddenWriting: Function0<Unit>? = null

    protected val maskValues = ArrayList<Int>()
    protected var maskStartIndex = -1
    protected var methodHandleInDefaultMethodIndex = -1

    init {
        sourceCompiler.initializeInlineFunctionContext(functionDescriptor)
        jvmSignature = typeMapper.mapSignatureWithGeneric(functionDescriptor, sourceCompiler.contextKind)
        isSameModule = sourceCompiler.isCallInsideSameModuleAsDeclared(functionDescriptor)

        if (functionDescriptor !is FictitiousArrayConstructor) {
            val functionOrAccessorName = typeMapper.mapAsmMethod(function).name
            //track changes for property accessor and @JvmName inline functions/property accessors
            if (functionOrAccessorName != functionDescriptor.name.asString()) {
                val scope = getMemberScope(functionDescriptor)
                //Fake lookup to track track changes for property accessors and @JvmName functions/property accessors
                scope?.getContributedFunctions(Name.identifier(functionOrAccessorName), sourceCompiler.lookupLocation)
            }
        }
    }


    protected fun throwCompilationException(
        nodeAndSmap: SMAPAndMethodNode?, e: Exception, generateNodeText: Boolean
    ): CompilationException {
        val contextDescriptor = sourceCompiler.compilationContextDescriptor
        val element = DescriptorToSourceUtils.descriptorToDeclaration(contextDescriptor)
        val node = nodeAndSmap?.node
        throw CompilationException(
            "Couldn't inline method call '" + functionDescriptor.name + "' into\n" +
                    DescriptorRenderer.DEBUG_TEXT.render(contextDescriptor) + "\n" +
                    (element?.text ?: "<no source>") +
                    if (generateNodeText) "\nCause: " + node.nodeText else "",
            e, sourceCompiler.callElement as? PsiElement
        )
    }

    protected fun generateStub(resolvedCall: ResolvedCall<*>?, codegen: BaseExpressionCodegen) {
        leaveTemps()
        assert(resolvedCall != null)
        val message = "Call is part of inline cycle: " + resolvedCall!!.call.callElement.text
        AsmUtil.genThrow(codegen.v, "java/lang/UnsupportedOperationException", message)
    }

    protected fun endCall(result: InlineResult) {
        leaveTemps()

        codegen.propagateChildReifiedTypeParametersUsages(result.reifiedTypeParametersUsages)

        state.factory.removeClasses(result.calcClassesToRemove())

        codegen.markLineNumberAfterInlineIfNeeded()
    }

    fun performInline(
        typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
        callDefault: Boolean,
        codegen: BaseExpressionCodegen
    ) {
        var nodeAndSmap: SMAPAndMethodNode? = null
        try {
            nodeAndSmap = createInlineMethodNode(functionDescriptor, jvmSignature, callDefault, typeArguments, state, sourceCompiler)
            endCall(inlineCall(nodeAndSmap, callDefault))
        } catch (e: CompilationException) {
            throw e
        } catch (e: InlineException) {
            throw throwCompilationException(nodeAndSmap, e, false)
        } catch (e: Exception) {
            throw throwCompilationException(nodeAndSmap, e, true)
        }
    }

    private fun canSkipStackSpillingOnInline(methodNode: MethodNode): Boolean {
        // Temporary disable this optimization until
        // https://issuetracker.google.com/issues/68796377 is fixed
        // or until d8 substitute dex
        return false

        // Stack spilling before inline function 'f' call is required if:
        //  - 'f' is a suspend function
        //  - 'f' has try-catch blocks
        //  - 'f' has loops
        //
        // Instead of checking for loops precisely, we just check if there are any backward jumps -
        // that is, a jump from instruction #i to instruction #j where j < i

        if (functionDescriptor.isSuspend) return false
        if (methodNode.tryCatchBlocks.isNotEmpty()) return false

        fun isBackwardJump(fromIndex: Int, toLabel: LabelNode) =
            methodNode.instructions.indexOf(toLabel) < fromIndex

        val insns = methodNode.instructions.toArray()
        for (i in insns.indices) {
            val insn = insns[i]
            when (insn) {
                is JumpInsnNode ->
                    if (isBackwardJump(i, insn.label)) return false

                is LookupSwitchInsnNode -> {
                    insn.dflt?.let {
                        if (isBackwardJump(i, it)) return false
                    }
                    if (insn.labels.any { isBackwardJump(i, it) }) return false
                }

                is TableSwitchInsnNode -> {
                    insn.dflt?.let {
                        if (isBackwardJump(i, it)) return false
                    }
                    if (insn.labels.any { isBackwardJump(i, it) }) return false
                }
            }
        }

        return true
    }

    private fun continuationValue(): StackValue {
        assert(codegen is ExpressionCodegen) { "Expected ExpressionCodegen in coroutineContext inlining" }
        codegen as ExpressionCodegen

        val parentContext = codegen.context.parentContext
        return if (parentContext is ClosureContext) {
            val originalSuspendLambdaDescriptor =
                parentContext.originalSuspendLambdaDescriptor ?: error("No original lambda descriptor found")
            codegen.genCoroutineInstanceForSuspendLambda(originalSuspendLambdaDescriptor)
                    ?: error("No stack value for coroutine instance of lambda found")
        } else
            codegen.getContinuationParameterFromEnclosingSuspendFunctionDescriptor(codegen.context.functionDescriptor)
                    ?: error("No stack value for continuation parameter of suspend function")
    }

    protected fun inlineCall(nodeAndSmap: SMAPAndMethodNode, callDefault: Boolean): InlineResult {
        assert(delayedHiddenWriting == null) { "'putHiddenParamsIntoLocals' should be called after 'processAndPutHiddenParameters(true)'" }
        defaultSourceMapper.callSiteMarker = CallSiteMarker(codegen.lastLineNumber)
        val node = nodeAndSmap.node
        if (callDefault) {
            val defaultLambdas = expandMaskConditionsAndUpdateVariableNodes(
                node, maskStartIndex, maskValues, methodHandleInDefaultMethodIndex,
                extractDefaultLambdaOffsetAndDescriptor(jvmSignature, functionDescriptor)
            )
            for (lambda in defaultLambdas) {
                invocationParamBuilder.buildParameters().getParameterByDeclarationSlot(lambda.offset).lambda = lambda
                val prev = expressionMap.put(lambda.offset, lambda)
                assert(prev == null) { "Lambda with offset ${lambda.offset} already exists: $prev" }
            }
        }
        val reificationResult = reifiedTypeInliner.reifyInstructions(node)
        generateClosuresBodies()

        //through generation captured parameters will be added to invocationParamBuilder
        putClosureParametersOnStack()

        val shouldSpillStack = !canSkipStackSpillingOnInline(node)

        if (shouldSpillStack) {
            addInlineMarker(codegen.v, true)
        }

        val parameters = invocationParamBuilder.buildParameters()

        val info = RootInliningContext(
            expressionMap, state, codegen.inlineNameGenerator.subGenerator(jvmSignature.asmMethod.name),
            sourceCompiler, sourceCompiler.inlineCallSiteInfo, reifiedTypeInliner, typeParameterMappings
        )

        val inliner = MethodInliner(
            node, parameters, info, FieldRemapper(null, null, parameters), isSameModule,
            "Method inlining " + sourceCompiler.callElementText,
            createNestedSourceMapper(nodeAndSmap, defaultSourceMapper), info.callSiteInfo,
            if (functionDescriptor.isInlineOnly()) InlineOnlySmapSkipper(codegen) else null,
            !isInlinedToInlineFunInKotlinRuntime()
        ) //with captured

        val remapper = LocalVarRemapper(parameters, initialFrameSize)

        val adapter = createEmptyMethodNode()
        //hack to keep linenumber info, otherwise jdi will skip begin of linenumber chain
        adapter.visitInsn(Opcodes.NOP)

        val result = inliner.doInline(adapter, remapper, true, LabelOwner.SKIP_ALL)
        result.reifiedTypeParametersUsages.mergeAll(reificationResult)

        val labels = sourceCompiler.getContextLabels()

        val infos = MethodInliner.processReturns(adapter, LabelOwner { labels.contains(it) }, true, null)
        sourceCompiler.generateAndInsertFinallyBlocks(
            adapter, infos, (remapper.remap(parameters.argsSizeOnStack + 1).value as StackValue.Local).index
        )
        removeStaticInitializationTrigger(adapter)
        if (!sourceCompiler.isFinallyMarkerRequired()) {
            removeFinallyMarkers(adapter)
        }

        adapter.accept(MethodBodyVisitor(codegen.v))

        if (shouldSpillStack) {
            addInlineMarker(codegen.v, false)
        }

        defaultSourceMapper.callSiteMarker = null

        return result
    }

    private fun isInlinedToInlineFunInKotlinRuntime(): Boolean {
        val codegen = this.codegen as? ExpressionCodegen ?: return false
        val caller = codegen.context.functionDescriptor
        if (!caller.isInline) return false
        val callerPackage = DescriptorUtils.getParentOfType(caller, PackageFragmentDescriptor::class.java) ?: return false
        return callerPackage.fqName.asString().startsWith("kotlin.")
    }

    private fun generateClosuresBodies() {
        for (info in expressionMap.values) {
            info.generateLambdaBody(sourceCompiler, reifiedTypeInliner)
        }
    }

    protected fun putArgumentOrCapturedToLocalVal(
        jvmKotlinType: JvmKotlinType,
        stackValue: StackValue,
        capturedParamIndex: Int,
        parameterIndex: Int,
        kind: ValueKind
    ) {
        val isDefaultParameter = kind === ValueKind.DEFAULT_PARAMETER
        val jvmType = jvmKotlinType.type
        val kotlinType = jvmKotlinType.kotlinType
        if (!isDefaultParameter && shouldPutGeneralValue(jvmType, kotlinType, stackValue)) {
            stackValue.put(jvmType, kotlinType, codegen.v)
        }

        if (!asFunctionInline && Type.VOID_TYPE !== jvmType) {
            //TODO remap only inlinable closure => otherwise we could get a lot of problem
            val couldBeRemapped = !shouldPutGeneralValue(jvmType, kotlinType, stackValue) && kind !== ValueKind.DEFAULT_PARAMETER
            val remappedValue = if (couldBeRemapped) stackValue else null

            val info: ParameterInfo
            if (capturedParamIndex >= 0) {
                val capturedParamInfoInLambda = activeLambda!!.capturedVars[capturedParamIndex]
                info = invocationParamBuilder.addCapturedParam(capturedParamInfoInLambda, capturedParamInfoInLambda.fieldName, false)
                info.setRemapValue(remappedValue)
            } else {
                info = invocationParamBuilder.addNextValueParameter(jvmType, false, remappedValue, parameterIndex)
            }

            recordParameterValueInLocalVal(
                false,
                isDefaultParameter || kind === ValueKind.DEFAULT_LAMBDA_CAPTURED_PARAMETER,
                info
            )
        }
    }

    protected fun recordParameterValueInLocalVal(
        delayedWritingToLocals: Boolean,
        skipStore: Boolean,
        vararg infos: ParameterInfo
    ): Function0<Unit>? {
        val index = IntArray(infos.size) { i ->
            if (!infos[i].isSkippedOrRemapped) {
                codegen.frameMap.enterTemp(infos[i].getType())
            } else -1
        }

        val possibleLazyTask = {
            for (i in infos.indices.reversed()) {
                val info = infos[i]
                if (!info.isSkippedOrRemapped) {
                    val type = info.type
                    val local = StackValue.local(index[i], type)
                    if (!skipStore) {
                        local.store(StackValue.onStack(type), codegen.v)
                    }
                    if (info is CapturedParamInfo) {
                        info.setRemapValue(local)
                        info.isSynthetic = true
                    }
                }
            }
        }

        if (delayedWritingToLocals) return possibleLazyTask
        possibleLazyTask()
        return null
    }


    private fun leaveTemps() {
        invocationParamBuilder.listAllParams().asReversed().forEach { param ->
            if (!param.isSkippedOrRemapped || CapturedParamInfo.isSynthetic(param)) {
                codegen.frameMap.leaveTemp(param.type)
            }
        }
    }

    private fun putClosureParametersOnStack() {
        for (next in expressionMap.values) {
            //closure parameters for bounded callable references are generated inplace
            if (next is ExpressionLambda && next.isBoundCallableReference) continue
            putClosureParametersOnStack(next, null)
        }
    }

    abstract protected fun putClosureParametersOnStack(next: LambdaInfo, functionReferenceReceiver: StackValue?)

    protected fun rememberCapturedForDefaultLambda(defaultLambda: DefaultLambda) {
        for ((paramIndex, captured) in defaultLambda.capturedVars.withIndex()) {
            putArgumentOrCapturedToLocalVal(
                JvmKotlinType(captured.type),
                //HACK: actually parameter would be placed on stack in default function
                // also see ValueKind.DEFAULT_LAMBDA_CAPTURED_PARAMETER check
                StackValue.onStack(captured.type),
                paramIndex,
                paramIndex,
                ValueKind.DEFAULT_LAMBDA_CAPTURED_PARAMETER
            )

            defaultLambda.parameterOffsetsInDefault.add(invocationParamBuilder.nextParameterOffset)
        }
    }


    protected fun processDefaultMaskOrMethodHandler(value: StackValue, kind: ValueKind): Boolean {
        if (kind !== ValueKind.DEFAULT_MASK && kind !== ValueKind.METHOD_HANDLE_IN_DEFAULT) {
            return false
        }
        assert(value is StackValue.Constant) { "Additional default method argument should be constant, but " + value }
        val constantValue = (value as StackValue.Constant).value
        if (kind === ValueKind.DEFAULT_MASK) {
            assert(constantValue is Int) { "Mask should be of Integer type, but " + constantValue }
            maskValues.add(constantValue as Int)
            if (maskStartIndex == -1) {
                maskStartIndex = invocationParamBuilder.listAllParams().sumBy {
                    if (it is CapturedParamInfo) 0 else it.type.size
                }
            }
        } else {
            assert(constantValue == null) { "Additional method handle for default argument should be null, but " + constantValue!! }
            methodHandleInDefaultMethodIndex = maskStartIndex + maskValues.size
        }
        return true
    }


    companion object {

        private fun getMemberScope(functionOrAccessor: FunctionDescriptor): MemberScope? {
            val callableMemberDescriptor = JvmCodegenUtil.getDirectMember(functionOrAccessor)
            val classOrPackageFragment = callableMemberDescriptor.containingDeclaration
            return when (classOrPackageFragment) {
                is ClassDescriptor -> classOrPackageFragment.unsubstitutedMemberScope
                is PackageFragmentDescriptor -> classOrPackageFragment.getMemberScope()
                else -> null
            }
        }

        internal fun createInlineMethodNode(
            functionDescriptor: FunctionDescriptor,
            jvmSignature: JvmMethodSignature,
            callDefault: Boolean,
            typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
            state: GenerationState,
            sourceCompilerForInline: SourceCompilerForInline
        ): SMAPAndMethodNode {
            val languageVersionSettings = state.languageVersionSettings
            when {
                isSpecialEnumMethod(functionDescriptor) -> {
                    val node = createSpecialEnumMethodBody(
                        functionDescriptor.name.asString(),
                        typeArguments!!.keys.single().defaultType,
                        state.typeMapper
                    )
                    return SMAPAndMethodNode(node, createDefaultFakeSMAP())
                }
                functionDescriptor.isBuiltInIntercepted(languageVersionSettings) ->
                    return SMAPAndMethodNode(
                        createMethodNodeForIntercepted(functionDescriptor, state.typeMapper, languageVersionSettings),
                        createDefaultFakeSMAP()
                    )
                functionDescriptor.isBuiltInCoroutineContext(languageVersionSettings) ->
                    return SMAPAndMethodNode(
                        createMethodNodeForCoroutineContext(functionDescriptor, languageVersionSettings),
                        createDefaultFakeSMAP()
                    )
                functionDescriptor.isBuiltInSuspendCoroutineUninterceptedOrReturnInJvm(languageVersionSettings) ->
                    return SMAPAndMethodNode(
                        createMethodNodeForSuspendCoroutineUninterceptedOrReturn(
                            functionDescriptor,
                            state.typeMapper,
                            languageVersionSettings
                        ),
                        createDefaultFakeSMAP()
                    )
                functionDescriptor.isBuiltinAlwaysEnabledAssert() ->
                    return SMAPAndMethodNode(
                        createMethodNodeForAlwaysEnabledAssert(functionDescriptor, state.typeMapper),
                        createDefaultFakeSMAP()
                    )
            }

            val asmMethod = if (callDefault)
                state.typeMapper.mapDefaultMethod(functionDescriptor, sourceCompilerForInline.contextKind)
            else
                mangleSuspendInlineFunctionAsmMethodIfNeeded(functionDescriptor, jvmSignature.asmMethod)

            val owner = state.typeMapper.mapImplementationOwner(functionDescriptor)
            val methodId = MethodId(owner.internalName, asmMethod)
            val directMember = getDirectMemberAndCallableFromObject(functionDescriptor)
            if (!isBuiltInArrayIntrinsic(functionDescriptor) && directMember !is DeserializedCallableMemberDescriptor) {
                return sourceCompilerForInline.doCreateMethodNodeFromSource(functionDescriptor, jvmSignature, callDefault, asmMethod)
            }

            val resultInCache = state.inlineCache.methodNodeById.getOrPut(methodId) {
                val result = doCreateMethodNodeFromCompiled(directMember, state, asmMethod)
                        ?: if (functionDescriptor.isSuspend)
                            doCreateMethodNodeFromCompiled(directMember, state, jvmSignature.asmMethod)
                        else
                            null
                result ?: throw IllegalStateException("Couldn't obtain compiled function body for $functionDescriptor")
            }

            return resultInCache.copyWithNewNode(cloneMethodNode(resultInCache.node))
        }

        private fun createDefaultFakeSMAP() = SMAPParser.parseOrCreateDefault(null, null, "fake", -1, -1)

        // For suspend inline functions we generate two methods:
        // 1) normal one: with state machine to call directly
        // 2) for inliner: with mangled name and without state machine
        private fun mangleSuspendInlineFunctionAsmMethodIfNeeded(functionDescriptor: FunctionDescriptor, asmMethod: Method): Method {
            if (!functionDescriptor.isSuspend) return asmMethod
            return Method("${asmMethod.name}\$\$forInline", asmMethod.descriptor)
        }

        private fun getDirectMemberAndCallableFromObject(functionDescriptor: FunctionDescriptor): CallableMemberDescriptor {
            val directMember = JvmCodegenUtil.getDirectMember(functionDescriptor)
            return (directMember as? ImportedFromObjectCallableDescriptor<*>)?.callableFromObject ?: directMember
        }

        private fun cloneMethodNode(methodNode: MethodNode): MethodNode {
            methodNode.instructions.resetLabels()
            return MethodNode(
                API, methodNode.access, methodNode.name, methodNode.desc, methodNode.signature,
                ArrayUtil.toStringArray(methodNode.exceptions)
            ).also(methodNode::accept)
        }

        private fun doCreateMethodNodeFromCompiled(
            callableDescriptor: CallableMemberDescriptor,
            state: GenerationState,
            asmMethod: Method
        ): SMAPAndMethodNode? {
            if (isBuiltInArrayIntrinsic(callableDescriptor)) {
                val classId = classId
                val bytes = state.inlineCache.classBytes.getOrPut(classId) { bytecode }
                return getMethodNode(bytes, asmMethod.name, asmMethod.descriptor, AsmUtil.asmTypeByClassId(classId))
            }

            assert(callableDescriptor is DeserializedCallableMemberDescriptor) { "Not a deserialized function or proper: " + callableDescriptor }

            val containingClasses =
                KotlinTypeMapper.getContainingClassesForDeserializedCallable(callableDescriptor as DeserializedCallableMemberDescriptor)

            val containerId = containingClasses.implClassId

            val bytes = state.inlineCache.classBytes.getOrPut(containerId) {
                findVirtualFile(state, containerId)?.contentsToByteArray()
                        ?: throw IllegalStateException("Couldn't find declaration file for " + containerId)
            }

            val methodNode =
                getMethodNode(bytes, asmMethod.name, asmMethod.descriptor, AsmUtil.asmTypeByClassId(containerId)) ?: return null

            // KLUDGE: Inline suspend function built with compiler version less than 1.1.4/1.2-M1 did not contain proper
            // before/after suspension point marks, so we detect those functions here and insert the corresponding marks
            if (isLegacySuspendInlineFunction(callableDescriptor)) {
                insertLegacySuspendInlineMarks(methodNode.node)
            }

            return methodNode
        }

        private fun isBuiltInArrayIntrinsic(callableDescriptor: CallableMemberDescriptor): Boolean {
            if (callableDescriptor is FictitiousArrayConstructor) return true
            val name = callableDescriptor.name.asString()
            return (name == "arrayOf" || name == "emptyArray") && callableDescriptor.containingDeclaration is BuiltInsPackageFragment
        }

        private fun removeStaticInitializationTrigger(methodNode: MethodNode) {
            val insnList = methodNode.instructions
            var insn: AbstractInsnNode? = insnList.first
            while (insn != null) {
                if (MultifileClassPartCodegen.isStaticInitTrigger(insn)) {
                    val clinitTriggerCall = insn
                    insn = insn.next
                    insnList.remove(clinitTriggerCall)
                } else {
                    insn = insn.next
                }
            }
        }


        /*descriptor is null for captured vars*/
        private fun shouldPutGeneralValue(type: Type, kotlinType: KotlinType?, stackValue: StackValue): Boolean {
            //remap only inline functions (and maybe non primitives)
            //TODO - clean assertion and remapping logic

            // don't remap boxing/unboxing primitives
            if (isPrimitive(type) != isPrimitive(stackValue.type)) {
                return true
            }

            // don't remap boxing/unboxing inline classes
            if (StackValue.requiresInlineClassBoxingOrUnboxing(stackValue.type, stackValue.kotlinType, type, kotlinType)) {
                return true
            }

            if (stackValue is StackValue.Local) {
                return false
            }

            var field = stackValue
            if (stackValue is StackValue.FieldForSharedVar) {
                field = stackValue.receiver
            }

            //check that value corresponds to captured inlining parameter
            if (field is StackValue.Field) {
                val varDescriptor = field.descriptor
                //check that variable is inline function parameter
                return !(varDescriptor is ParameterDescriptor &&
                        InlineUtil.isInlineParameter(varDescriptor) &&
                        InlineUtil.isInline(varDescriptor.containingDeclaration))
            }

            return true
        }


        fun getDeclarationLabels(lambdaOrFun: PsiElement?, descriptor: DeclarationDescriptor): Set<String> {
            val result = HashSet<String>()

            if (lambdaOrFun != null) {
                val label = LabelResolver.getLabelNameIfAny(lambdaOrFun)
                if (label != null) {
                    result.add(label.asString())
                }
            }

            if (!isFunctionLiteral(descriptor)) {
                if (!descriptor.name.isSpecial) {
                    result.add(descriptor.name.asString())
                }
                result.add(FIRST_FUN_LABEL)
            }
            return result
        }


        fun createNestedSourceMapper(nodeAndSmap: SMAPAndMethodNode, parent: SourceMapper): SourceMapper {
            return NestedSourceMapper(parent, nodeAndSmap.sortedRanges, nodeAndSmap.classSMAP.sourceInfo)
        }
    }
}

val BaseExpressionCodegen.v: InstructionAdapter
    get() = visitor

class PsiInlineCodegen(
    codegen: ExpressionCodegen,
    state: GenerationState,
    function: FunctionDescriptor,
    typeParameterMappings: TypeParameterMappings,
    sourceCompiler: SourceCompilerForInline
) : InlineCodegen<ExpressionCodegen>(codegen, state, function, typeParameterMappings, sourceCompiler), CallGenerator {

    override fun genCallInner(
        callableMethod: Callable,
        resolvedCall: ResolvedCall<*>?,
        callDefault: Boolean,
        codegen: ExpressionCodegen
    ) {
        if (!state.globalInlineContext.enterIntoInlining(resolvedCall)) {
            generateStub(resolvedCall, codegen)
            return
        }
        try {
            performInline(resolvedCall?.typeArguments, callDefault, codegen)
        } finally {
            state.globalInlineContext.exitFromInliningOf(resolvedCall)
        }
    }

    override fun processAndPutHiddenParameters(justProcess: Boolean) {
        if (getMethodAsmFlags(functionDescriptor, sourceCompiler.contextKind, state) and Opcodes.ACC_STATIC == 0) {
            invocationParamBuilder.addNextParameter(AsmTypes.OBJECT_TYPE, false)
        }

        for (param in jvmSignature.valueParameters) {
            if (param.kind == JvmMethodParameterKind.VALUE) {
                break
            }
            invocationParamBuilder.addNextParameter(param.asmType, false)
        }

        invocationParamBuilder.markValueParametersStart()
        val hiddenParameters = invocationParamBuilder.buildParameters().parameters

        delayedHiddenWriting = recordParameterValueInLocalVal(justProcess, false, *hiddenParameters.toTypedArray())
    }

    override fun putClosureParametersOnStack(next: LambdaInfo, functionReferenceReceiver: StackValue?) {
        activeLambda = next
        when (next) {
            is PsiExpressionLambda -> codegen.pushClosureOnStack(next.classDescriptor, true, this, functionReferenceReceiver)
            is DefaultLambda -> rememberCapturedForDefaultLambda(next)
            else -> throw RuntimeException("Unknown lambda: $next")
        }
        activeLambda = null
    }

    private fun getBoundCallableReferenceReceiver(argumentExpression: KtExpression): ReceiverValue? {
        val deparenthesized = KtPsiUtil.deparenthesize(argumentExpression) as? KtCallableReferenceExpression ?: return null
        val resolvedCall = deparenthesized.callableReference.getResolvedCallWithAssert(state.bindingContext)
        return JvmCodegenUtil.getBoundCallableReferenceReceiver(resolvedCall)
    }

    /*lambda or callable reference*/
    private fun isInliningParameter(expression: KtExpression, valueParameterDescriptor: ValueParameterDescriptor): Boolean {
        //TODO deparenthisise typed
        val deparenthesized = KtPsiUtil.deparenthesize(expression)

        return InlineUtil.isInlineParameter(valueParameterDescriptor) && isInlinableParameterExpression(deparenthesized)
    }

    override fun genValueAndPut(
        valueParameterDescriptor: ValueParameterDescriptor,
        argumentExpression: KtExpression,
        parameterType: Type,
        parameterIndex: Int
    ) {
        if (isInliningParameter(argumentExpression, valueParameterDescriptor)) {
            val lambdaInfo = rememberClosure(argumentExpression, parameterType, valueParameterDescriptor)

            val receiverValue = getBoundCallableReferenceReceiver(argumentExpression)
            if (receiverValue != null) {
                val receiver = codegen.generateReceiverValue(receiverValue, false)
                putClosureParametersOnStack(
                    lambdaInfo,
                    StackValue.coercion(receiver, receiver.type.boxReceiverForBoundReference(), null)
                )
            }
        } else {
            val value = codegen.gen(argumentExpression)
            putValueIfNeeded(
                JvmKotlinType(parameterType, valueParameterDescriptor.original.type),
                value,
                ValueKind.GENERAL,
                valueParameterDescriptor.index
            )
        }
    }

    private fun rememberClosure(expression: KtExpression, type: Type, parameter: ValueParameterDescriptor): LambdaInfo {
        val ktLambda = KtPsiUtil.deparenthesize(expression)
        assert(isInlinableParameterExpression(ktLambda)) { "Couldn't find inline expression in ${expression.text}" }

        return PsiExpressionLambda(
            ktLambda!!, typeMapper, state.languageVersionSettings,
            parameter.isCrossinline, getBoundCallableReferenceReceiver(expression) != null
        ).also { lambda ->
            val closureInfo = invocationParamBuilder.addNextValueParameter(type, true, null, parameter.index)
            closureInfo.lambda = lambda
            expressionMap.put(closureInfo.index, lambda)
        }
    }

    override fun putValueIfNeeded(parameterType: JvmKotlinType, value: StackValue, kind: ValueKind, parameterIndex: Int) {
        if (processDefaultMaskOrMethodHandler(value, kind)) return

        assert(maskValues.isEmpty()) { "Additional default call arguments should be last ones, but " + value }

        putArgumentOrCapturedToLocalVal(parameterType, value, -1, parameterIndex, kind)
    }

    override fun putCapturedValueOnStack(stackValue: StackValue, valueType: Type, paramIndex: Int) {
        putArgumentOrCapturedToLocalVal(
            JvmKotlinType(stackValue.type, stackValue.kotlinType), stackValue, paramIndex, paramIndex, ValueKind.CAPTURED
        )
    }

    override fun reorderArgumentsIfNeeded(actualArgsWithDeclIndex: List<ArgumentAndDeclIndex>, valueParameterTypes: List<Type>) = Unit

    override fun putHiddenParamsIntoLocals() {
        assert(delayedHiddenWriting != null) { "processAndPutHiddenParameters(true) should be called before putHiddenParamsIntoLocals" }
        delayedHiddenWriting!!.invoke()
        delayedHiddenWriting = null
    }
}
