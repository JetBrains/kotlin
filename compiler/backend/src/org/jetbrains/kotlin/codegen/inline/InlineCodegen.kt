/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import com.intellij.psi.PsiElement
import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.AsmUtil.isPrimitive
import org.jetbrains.kotlin.codegen.context.ClosureContext
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicArrayConstructors
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.ImportedFromObjectCallableDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.inline.isInlineOnly
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.resolve.jvm.requiresFunctionNameManglingForReturnType
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.isFunctionLiteral
import org.jetbrains.kotlin.types.expressions.LabelResolver
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.*
import java.util.*
import kotlin.collections.HashSet
import kotlin.math.max

abstract class InlineCodegen<out T : BaseExpressionCodegen>(
    protected val codegen: T,
    protected val state: GenerationState,
    protected val functionDescriptor: FunctionDescriptor,
    protected val methodOwner: Type,
    protected val jvmSignature: JvmMethodSignature,
    private val typeParameterMappings: TypeParameterMappings<*>,
    protected val sourceCompiler: SourceCompilerForInline,
    private val reifiedTypeInliner: ReifiedTypeInliner<*>
) {
    init {
        assert(InlineUtil.isInline(functionDescriptor)) {
            "InlineCodegen can inline only inline functions: $functionDescriptor"
        }
    }

    // TODO: implement AS_FUNCTION inline strategy
    private val asFunctionInline = false

    private val initialFrameSize = codegen.frameMap.currentSize

    private val isSameModule: Boolean

    protected val invocationParamBuilder = ParametersBuilder.newBuilder()

    protected val expressionMap = linkedMapOf<Int, FunctionalArgument>()

    var activeLambda: LambdaInfo? = null
        protected set

    private val sourceMapper = sourceCompiler.lazySourceMapper

    protected var delayedHiddenWriting: Function0<Unit>? = null

    protected val maskValues = ArrayList<Int>()
    protected var maskStartIndex = -1
    protected var methodHandleInDefaultMethodIndex = -1

    init {
        isSameModule = sourceCompiler.isCallInsideSameModuleAsDeclared(functionDescriptor)

        if (functionDescriptor !is FictitiousArrayConstructor) {
            //track changes for property accessor and @JvmName inline functions/property accessors
            if (jvmSignature.asmMethod.name != functionDescriptor.name.asString()) {
                trackLookup(functionDescriptor)
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

    protected fun endCall(result: InlineResult, registerLineNumberAfterwards: Boolean) {
        leaveTemps()

        codegen.propagateChildReifiedTypeParametersUsages(result.reifiedTypeParametersUsages)

        state.factory.removeClasses(result.calcClassesToRemove())

        codegen.markLineNumberAfterInlineIfNeeded(registerLineNumberAfterwards)
    }

    fun performInline(
        typeArguments: List<TypeParameterMarker>?,
        inlineDefaultLambdas: Boolean,
        mapDefaultSignature: Boolean,
        typeSystem: TypeSystemCommonBackendContext,
        registerLineNumberAfterwards: Boolean,
    ) {
        var nodeAndSmap: SMAPAndMethodNode? = null
        try {
            nodeAndSmap = createInlineMethodNode(mapDefaultSignature, typeArguments, typeSystem)
            endCall(inlineCall(nodeAndSmap, inlineDefaultLambdas), registerLineNumberAfterwards)
        } catch (e: CompilationException) {
            throw e
        } catch (e: InlineException) {
            throw throwCompilationException(nodeAndSmap, e, false)
        } catch (e: Exception) {
            throw throwCompilationException(nodeAndSmap, e, true)
        }
    }

    private fun canSkipStackSpillingOnInline(methodNode: MethodNode): Boolean {
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

    private fun inlineCall(nodeAndSmap: SMAPAndMethodNode, inlineDefaultLambda: Boolean): InlineResult {
        assert(delayedHiddenWriting == null) { "'putHiddenParamsIntoLocals' should be called after 'processAndPutHiddenParameters(true)'" }
        val node = nodeAndSmap.node
        if (inlineDefaultLambda) {
            for (lambda in extractDefaultLambdas(node)) {
                invocationParamBuilder.buildParameters().getParameterByDeclarationSlot(lambda.offset).functionalArgument = lambda
                val prev = expressionMap.put(lambda.offset, lambda)
                assert(prev == null) { "Lambda with offset ${lambda.offset} already exists: $prev" }
            }
        }
        val reificationResult = reifiedTypeInliner.reifyInstructions(node)
        generateClosuresBodies()

        //through generation captured parameters will be added to invocationParamBuilder
        putClosureParametersOnStack()

        val parameters = invocationParamBuilder.buildParameters()

        val info = RootInliningContext(
            expressionMap, state, codegen.inlineNameGenerator.subGenerator(jvmSignature.asmMethod.name),
            sourceCompiler, sourceCompiler.inlineCallSiteInfo, reifiedTypeInliner, typeParameterMappings
        )

        val sourceInfo = sourceMapper.sourceInfo!!
        val callSite = SourcePosition(codegen.lastLineNumber, sourceInfo.sourceFileName!!, sourceInfo.pathOrCleanFQN)
        val inliner = MethodInliner(
            node, parameters, info, FieldRemapper(null, null, parameters), isSameModule,
            "Method inlining " + sourceCompiler.callElementText,
            SourceMapCopier(sourceMapper, nodeAndSmap.classSMAP, callSite),
            info.callSiteInfo, if (functionDescriptor.isInlineOnly()) InlineOnlySmapSkipper(codegen) else null,
            !isInlinedToInlineFunInKotlinRuntime()
        ) //with captured

        val remapper = LocalVarRemapper(parameters, initialFrameSize)

        val adapter = createEmptyMethodNode()
        //hack to keep linenumber info, otherwise jdi will skip begin of linenumber chain
        adapter.visitInsn(Opcodes.NOP)

        val result = inliner.doInline(adapter, remapper, true, mapOf())
        result.reifiedTypeParametersUsages.mergeAll(reificationResult)

        val infos = MethodInliner.processReturns(adapter, sourceCompiler.getContextLabels(), null)
        generateAndInsertFinallyBlocks(
            adapter, infos, (remapper.remap(parameters.argsSizeOnStack + 1).value as StackValue.Local).index
        )
        if (!sourceCompiler.isFinallyMarkerRequired()) {
            removeFinallyMarkers(adapter)
        }

        // In case `codegen.v` is `<clinit>`, initializer for the `$assertionsDisabled` field
        // needs to be inserted before the code that actually uses it.
        generateAssertFieldIfNeeded(info)

        val shouldSpillStack = !canSkipStackSpillingOnInline(node)
        if (shouldSpillStack) {
            addInlineMarker(codegen.v, true)
        }
        adapter.accept(MethodBodyVisitor(codegen.v))
        if (shouldSpillStack) {
            addInlineMarker(codegen.v, false)
        }
        return result
    }

    abstract fun extractDefaultLambdas(node: MethodNode): List<DefaultLambda>

    abstract fun descriptorIsDeserialized(memberDescriptor: CallableMemberDescriptor): Boolean

    fun generateAndInsertFinallyBlocks(
        intoNode: MethodNode,
        insertPoints: List<MethodInliner.PointForExternalFinallyBlocks>,
        offsetForFinallyLocalVar: Int
    ) {
        if (!sourceCompiler.hasFinallyBlocks()) return

        val extensionPoints = HashMap<AbstractInsnNode, MethodInliner.PointForExternalFinallyBlocks>()
        for (insertPoint in insertPoints) {
            extensionPoints.put(insertPoint.beforeIns, insertPoint)
        }

        val processor = DefaultProcessor(intoNode, offsetForFinallyLocalVar)

        var curFinallyDepth = 0
        var curInstr: AbstractInsnNode? = intoNode.instructions.first
        while (curInstr != null) {
            processor.processInstruction(curInstr, true)
            if (isFinallyStart(curInstr)) {
                //TODO depth index calc could be more precise
                curFinallyDepth = getConstant(curInstr.previous)
            }

            val extension = extensionPoints[curInstr]
            if (extension != null) {
                val start = Label()

                val finallyNode = createEmptyMethodNode()
                finallyNode.visitLabel(start)

                val finallyCodegen =
                    sourceCompiler.createCodegenForExternalFinallyBlockGenerationOnNonLocalReturn(finallyNode, curFinallyDepth)

                val frameMap = finallyCodegen.frameMap
                val mark = frameMap.mark()
                var marker = -1
                val intervals = processor.localVarsMetaInfo.currentIntervals
                for (interval in intervals) {
                    marker = max(interval.node.index + 1, marker)
                }
                while (frameMap.currentSize < max(processor.nextFreeLocalIndex, offsetForFinallyLocalVar + marker)) {
                    frameMap.enterTemp(Type.INT_TYPE)
                }

                sourceCompiler.generateFinallyBlocksIfNeeded(
                    finallyCodegen, extension.returnType, extension.finallyIntervalEnd.label, extension.jumpTarget
                )

                //Exception table for external try/catch/finally blocks will be generated in original codegen after exiting this method
                insertNodeBefore(finallyNode, intoNode, curInstr)

                val splitBy = SimpleInterval(start.info as LabelNode, extension.finallyIntervalEnd)
                processor.tryBlocksMetaInfo.splitAndRemoveCurrentIntervals(splitBy, true)

                //processor.getLocalVarsMetaInfo().splitAndRemoveIntervalsFromCurrents(splitBy);

                mark.dropTo()
            }

            curInstr = curInstr.next
        }

        processor.substituteTryBlockNodes(intoNode)

        //processor.substituteLocalVarTable(intoNode);
    }

    protected abstract fun generateAssertFieldIfNeeded(info: RootInliningContext)

    private fun isInlinedToInlineFunInKotlinRuntime(): Boolean {
        val codegen = this.codegen as? ExpressionCodegen ?: return false
        val caller = codegen.context.functionDescriptor
        if (!caller.isInline) return false
        val callerPackage = DescriptorUtils.getParentOfType(caller, PackageFragmentDescriptor::class.java) ?: return false
        return callerPackage.fqName.asString().let {
            // package either equals to 'kotlin' or starts with 'kotlin.'
            it.startsWith("kotlin") && (it.length <= 6 || it[6] == '.')
        }
    }

    private fun generateClosuresBodies() {
        for (info in expressionMap.values) {
            if (info is LambdaInfo) {
                info.generateLambdaBody(sourceCompiler, reifiedTypeInliner)
            }
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
                info.remapValue = remappedValue
            } else {
                info = invocationParamBuilder.addNextValueParameter(jvmType, false, remappedValue, parameterIndex)
                info.functionalArgument = when (kind) {
                    ValueKind.NON_INLINEABLE_ARGUMENT_FOR_INLINE_PARAMETER_CALLED_IN_SUSPEND ->
                        NonInlineableArgumentForInlineableParameterCalledInSuspend
                    ValueKind.NON_INLINEABLE_ARGUMENT_FOR_INLINE_SUSPEND_PARAMETER -> NonInlineableArgumentForInlineableSuspendParameter
                    else -> null
                }
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
                codegen.frameMap.enterTemp(infos[i].type)
            } else -1
        }

        val possibleLazyTask = {
            for (i in infos.indices.reversed()) {
                val info = infos[i]
                if (!info.isSkippedOrRemapped) {
                    val type = info.type
                    val local = StackValue.local(index[i], type)
                    if (!skipStore) {
                        local.store(StackValue.onStack(info.typeOnStack), codegen.v)
                    }
                    if (info is CapturedParamInfo) {
                        info.remapValue = local
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
            if (next is LambdaInfo) {
                if (next is ExpressionLambda && next.isBoundCallableReference) continue
                putClosureParametersOnStack(next, null)
            }
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
        assert(value is StackValue.Constant) {
            "Additional default method argument should be constant, but " + value
        }
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

    private fun trackLookup(functionOrAccessor: FunctionDescriptor) {
        val functionOrAccessorName = jvmSignature.asmMethod.name
        val lookupTracker = state.configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER) ?: return
        val location = sourceCompiler.lookupLocation.location ?: return
        val position = if (lookupTracker.requiresPosition) location.position else Position.NO_POSITION
        val classOrPackageFragment = functionOrAccessor.containingDeclaration
        lookupTracker.record(
            location.filePath,
            position,
            DescriptorUtils.getFqName(classOrPackageFragment).asString(),
            ScopeKind.CLASSIFIER,
            functionOrAccessorName
        )
    }


    internal fun createInlineMethodNode(
        callDefault: Boolean,
        typeArguments: List<TypeParameterMarker>?,
        typeSystem: TypeSystemCommonBackendContext
    ): SMAPAndMethodNode {
        val intrinsic = generateInlineIntrinsic(state, functionDescriptor, typeArguments, typeSystem)
        if (intrinsic != null) {
            return SMAPAndMethodNode(intrinsic, createDefaultFakeSMAP())
        }

        var asmMethod = mapMethod(callDefault)
        if (asmMethod.name.contains("-") &&
            !state.configuration.getBoolean(JVMConfigurationKeys.USE_OLD_INLINE_CLASSES_MANGLING_SCHEME) &&
            classFileContainsMethod(functionDescriptor, state, asmMethod) == false
        ) {
            state.typeMapper.useOldManglingRulesForFunctionAcceptingInlineClass = true
            asmMethod = mapMethod(callDefault)
            state.typeMapper.useOldManglingRulesForFunctionAcceptingInlineClass = false
        }

        val directMember = getDirectMemberAndCallableFromObject(functionDescriptor)
        if (!isBuiltInArrayIntrinsic(functionDescriptor) && !descriptorIsDeserialized(directMember)) {
            val node = sourceCompiler.doCreateMethodNodeFromSource(functionDescriptor, jvmSignature, callDefault, asmMethod)
            node.node.preprocessSuspendMarkers(forInline = true, keepFakeContinuation = false)
            return node
        }

        return getCompiledMethodNodeInner(functionDescriptor, directMember, asmMethod, methodOwner, state, jvmSignature)
    }

    private fun mapMethod(callDefault: Boolean): Method =
        if (callDefault) state.typeMapper.mapDefaultMethod(functionDescriptor, sourceCompiler.contextKind)
        else mangleSuspendInlineFunctionAsmMethodIfNeeded(functionDescriptor, jvmSignature.asmMethod)

    companion object {

        internal fun createSpecialInlineMethodNodeFromBinaries(functionDescriptor: FunctionDescriptor, state: GenerationState): MethodNode {
            val directMember = getDirectMemberAndCallableFromObject(functionDescriptor)
            assert(directMember is DescriptorWithContainerSource) {
                "Function is not in binaries: $functionDescriptor"
            }
            assert(directMember is FunctionDescriptor && directMember.isOperator) {
                "Operator function expected: $directMember"
            }

            val methodOwner = state.typeMapper.mapImplementationOwner(functionDescriptor)
            val jvmSignature = state.typeMapper.mapSignatureWithGeneric(functionDescriptor, OwnerKind.IMPLEMENTATION)

            val asmMethod = mangleSuspendInlineFunctionAsmMethodIfNeeded(functionDescriptor, jvmSignature.asmMethod)

            return getCompiledMethodNodeInner(functionDescriptor, directMember, asmMethod, methodOwner, state, jvmSignature).node
        }

        private fun getCompiledMethodNodeInner(
            functionDescriptor: FunctionDescriptor,
            directMember: CallableMemberDescriptor,
            asmMethod: Method,
            methodOwner: Type,
            state: GenerationState,
            jvmSignature: JvmMethodSignature
        ): SMAPAndMethodNode {
            val methodId = MethodId(methodOwner.internalName, asmMethod)

            val resultInCache = state.inlineCache.methodNodeById.getOrPut(methodId) {
                val result = doCreateMethodNodeFromCompiled(directMember, state, asmMethod)
                    ?: if (functionDescriptor.isSuspend)
                        doCreateMethodNodeFromCompiled(directMember, state, jvmSignature.asmMethod)
                    else
                        null
                result ?:
                throw IllegalStateException("Couldn't obtain compiled function body for $functionDescriptor")
            }

            return SMAPAndMethodNode(cloneMethodNode(resultInCache.node), resultInCache.classSMAP)
        }

        private fun createDefaultFakeSMAP() = SMAPParser.parseOrCreateDefault(null, null, "fake", -1, -1)

        // For suspend inline functions we generate two methods:
        // 1) normal one: with state machine to call directly
        // 2) for inliner: with mangled name and without state machine
        private fun mangleSuspendInlineFunctionAsmMethodIfNeeded(functionDescriptor: FunctionDescriptor, asmMethod: Method): Method {
            if (!functionDescriptor.isSuspend) return asmMethod
            return Method("${asmMethod.name}$FOR_INLINE_SUFFIX", asmMethod.descriptor)
        }

        private fun getDirectMemberAndCallableFromObject(functionDescriptor: FunctionDescriptor): CallableMemberDescriptor {
            val directMember = JvmCodegenUtil.getDirectMember(functionDescriptor)
            return (directMember as? ImportedFromObjectCallableDescriptor<*>)?.callableFromObject ?: directMember
        }

        private fun cloneMethodNode(methodNode: MethodNode): MethodNode {
            methodNode.instructions.resetLabels()
            return MethodNode(
                Opcodes.API_VERSION, methodNode.access, methodNode.name, methodNode.desc, methodNode.signature,
                ArrayUtil.toStringArray(methodNode.exceptions)
            ).also(methodNode::accept)
        }

        private fun doCreateMethodNodeFromCompiled(
            callableDescriptor: CallableMemberDescriptor,
            state: GenerationState,
            asmMethod: Method
        ): SMAPAndMethodNode? {
            if (isBuiltInArrayIntrinsic(callableDescriptor)) {
                val body = when {
                    callableDescriptor is FictitiousArrayConstructor -> IntrinsicArrayConstructors.generateArrayConstructorBody(asmMethod)
                    callableDescriptor.name.asString() == "emptyArray" -> IntrinsicArrayConstructors.generateEmptyArrayBody(asmMethod)
                    callableDescriptor.name.asString() == "arrayOf" -> IntrinsicArrayConstructors.generateArrayOfBody(asmMethod)
                    else -> throw UnsupportedOperationException("Not an array intrinsic: $callableDescriptor")
                }
                return SMAPAndMethodNode(body, SMAP(listOf()))
            }

            assert(callableDescriptor is DescriptorWithContainerSource) { "Not a deserialized function or proper: $callableDescriptor" }

            val containingClasses =
                KotlinTypeMapper.getContainingClassesForDeserializedCallable(callableDescriptor as DescriptorWithContainerSource)

            val containerId = containingClasses.implClassId

            val bytes = state.inlineCache.classBytes.getOrPut(containerId) {
                findVirtualFile(state, containerId)?.contentsToByteArray()
                    ?: throw IllegalStateException("Couldn't find declaration file for $containerId")
            }

            val methodNode = getMethodNodeInner(containerId, bytes, asmMethod, callableDescriptor) ?: return null

            // KLUDGE: Inline suspend function built with compiler version less than 1.1.4/1.2-M1 did not contain proper
            // before/after suspension point marks, so we detect those functions here and insert the corresponding marks
            if (isLegacySuspendInlineFunction(callableDescriptor)) {
                insertLegacySuspendInlineMarks(methodNode.node)
            }

            return methodNode
        }

        private fun getMethodNodeInner(
            containerId: ClassId,
            bytes: ByteArray,
            asmMethod: Method,
            callableDescriptor: CallableMemberDescriptor
        ): SMAPAndMethodNode? {
            val classType = AsmUtil.asmTypeByClassId(containerId)
            var methodNode = getMethodNode(bytes, asmMethod.name, asmMethod.descriptor, classType)
            if (methodNode == null && requiresFunctionNameManglingForReturnType(callableDescriptor)) {
                val nameWithoutManglingSuffix = asmMethod.name.stripManglingSuffixOrNull()
                if (nameWithoutManglingSuffix != null) {
                    methodNode = getMethodNode(bytes, nameWithoutManglingSuffix, asmMethod.descriptor, classType)
                }
                if (methodNode == null) {
                    val nameWithImplSuffix = "$nameWithoutManglingSuffix-impl"
                    methodNode = getMethodNode(bytes, nameWithImplSuffix, asmMethod.descriptor, classType)
                }
            }
            return methodNode
        }

        private fun String.stripManglingSuffixOrNull(): String? {
            val dashIndex = indexOf('-')
            return if (dashIndex < 0) null else substring(0, dashIndex)
        }

        private fun isBuiltInArrayIntrinsic(callableDescriptor: CallableMemberDescriptor): Boolean {
            if (callableDescriptor is FictitiousArrayConstructor) return true
            val name = callableDescriptor.name.asString()
            return (name == "arrayOf" || name == "emptyArray") && callableDescriptor.containingDeclaration.let { container ->
                container is PackageFragmentDescriptor && container.fqName == StandardNames.BUILT_INS_PACKAGE_FQ_NAME
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
    }

}

val BaseExpressionCodegen.v: InstructionAdapter
    get() = visitor
