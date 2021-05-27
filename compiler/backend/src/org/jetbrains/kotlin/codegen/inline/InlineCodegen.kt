/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.AsmUtil.isPrimitive
import org.jetbrains.kotlin.codegen.context.ClosureContext
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.inline.isInlineOnly
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.resolve.jvm.requiresFunctionNameManglingForReturnType
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.*
import kotlin.math.max

abstract class InlineCodegen<out T : BaseExpressionCodegen>(
    protected val codegen: T,
    protected val state: GenerationState,
    protected val functionDescriptor: FunctionDescriptor,
    protected val jvmSignature: JvmMethodSignature,
    private val typeParameterMappings: TypeParameterMappings<*>,
    protected val sourceCompiler: SourceCompilerForInline,
    protected val reifiedTypeInliner: ReifiedTypeInliner<*>
) {
    init {
        assert(InlineUtil.isInline(functionDescriptor)) {
            "InlineCodegen can inline only inline functions: $functionDescriptor"
        }
    }

    // TODO: implement AS_FUNCTION inline strategy
    private val asFunctionInline = false

    private val initialFrameSize = codegen.frameMap.currentSize

    protected val invocationParamBuilder = ParametersBuilder.newBuilder()
    protected val expressionMap = linkedMapOf<Int, FunctionalArgument>()
    protected val maskValues = ArrayList<Int>()
    protected var maskStartIndex = -1
    protected var methodHandleInDefaultMethodIndex = -1

    init {
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

    protected fun generateStub(text: String, codegen: BaseExpressionCodegen) {
        leaveTemps()
        AsmUtil.genThrow(codegen.visitor, "java/lang/UnsupportedOperationException", "Call is part of inline cycle: $text")
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
            when (val insn = insns[i]) {
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

    protected fun rememberClosure(parameterType: Type, index: Int, lambdaInfo: LambdaInfo) {
        val closureInfo = invocationParamBuilder.addNextValueParameter(parameterType, true, null, index)
        closureInfo.functionalArgument = lambdaInfo
        expressionMap[closureInfo.index] = lambdaInfo
    }

    private fun inlineCall(nodeAndSmap: SMAPAndMethodNode, inlineDefaultLambda: Boolean): InlineResult {
        val node = nodeAndSmap.node
        if (inlineDefaultLambda) {
            for (lambda in extractDefaultLambdas(node)) {
                invocationParamBuilder.buildParameters().getParameterByDeclarationSlot(lambda.offset).functionalArgument = lambda
                val prev = expressionMap.put(lambda.offset, lambda)
                assert(prev == null) { "Lambda with offset ${lambda.offset} already exists: $prev" }
                lambda.generateLambdaBody(sourceCompiler, reifiedTypeInliner)
                rememberCapturedForDefaultLambda(lambda)
            }
        }

        val reificationResult = reifiedTypeInliner.reifyInstructions(node)

        val parameters = invocationParamBuilder.buildParameters()

        val info = RootInliningContext(
            expressionMap, state, codegen.inlineNameGenerator.subGenerator(jvmSignature.asmMethod.name),
            sourceCompiler, sourceCompiler.inlineCallSiteInfo, reifiedTypeInliner, typeParameterMappings
        )

        val sourceMapper = sourceCompiler.sourceMapper
        val sourceInfo = sourceMapper.sourceInfo!!
        val callSite = SourcePosition(codegen.lastLineNumber, sourceInfo.sourceFileName!!, sourceInfo.pathOrCleanFQN)
        val inliner = MethodInliner(
            node, parameters, info, FieldRemapper(null, null, parameters), sourceCompiler.isCallInsideSameModuleAsCallee,
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
        if (!sourceCompiler.isFinallyMarkerRequired) {
            removeFinallyMarkers(adapter)
        }

        // In case `codegen.visitor` is `<clinit>`, initializer for the `$assertionsDisabled` field
        // needs to be inserted before the code that actually uses it.
        generateAssertFieldIfNeeded(info)

        val shouldSpillStack = !canSkipStackSpillingOnInline(node)
        if (shouldSpillStack) {
            addInlineMarker(codegen.visitor, true)
        }
        adapter.accept(MethodBodyVisitor(codegen.visitor))
        if (shouldSpillStack) {
            addInlineMarker(codegen.visitor, false)
        }
        return result
    }

    abstract fun extractDefaultLambdas(node: MethodNode): List<DefaultLambda>

    private fun generateAndInsertFinallyBlocks(
        intoNode: MethodNode,
        insertPoints: List<MethodInliner.PointForExternalFinallyBlocks>,
        offsetForFinallyLocalVar: Int
    ) {
        if (!sourceCompiler.hasFinallyBlocks()) return

        val extensionPoints = insertPoints.associateBy { it.beforeIns }
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
                processor.localVarsMetaInfo.splitAndRemoveCurrentIntervals(splitBy, true)

                mark.dropTo()
            }

            curInstr = curInstr.next
        }

        processor.substituteTryBlockNodes(intoNode)
        processor.substituteLocalVarTable(intoNode)
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

    protected fun putArgumentOrCapturedToLocalVal(
        jvmKotlinType: JvmKotlinType,
        stackValue: StackValue,
        capturedParam: CapturedParamDesc?,
        parameterIndex: Int,
        kind: ValueKind
    ) {
        val isDefaultParameter = kind === ValueKind.DEFAULT_PARAMETER
        val jvmType = jvmKotlinType.type
        val kotlinType = jvmKotlinType.kotlinType
        val canRemap = isPrimitive(jvmType) == isPrimitive(stackValue.type) &&
                !StackValue.requiresInlineClassBoxingOrUnboxing(stackValue.type, stackValue.kotlinType, jvmType, kotlinType) &&
                (stackValue is StackValue.Local || stackValue.isCapturedInlineParameter())
        if (!canRemap && !isDefaultParameter) {
            stackValue.put(jvmType, kotlinType, codegen.visitor)
        }

        if (!asFunctionInline && Type.VOID_TYPE !== jvmType) {
            //TODO remap only inlinable closure => otherwise we could get a lot of problem
            val remappedValue = if (canRemap && !isDefaultParameter) stackValue else null
            val info: ParameterInfo
            if (capturedParam != null) {
                info = invocationParamBuilder.addCapturedParam(capturedParam, capturedParam.fieldName, false)
                info.remapValue = remappedValue
            } else {
                info = invocationParamBuilder.addNextValueParameter(jvmType, false, remappedValue, parameterIndex)
                info.functionalArgument = when (kind) {
                    ValueKind.NON_INLINEABLE_ARGUMENT_FOR_INLINE_PARAMETER_CALLED_IN_SUSPEND ->
                        NonInlineableArgumentForInlineableParameterCalledInSuspend
                    ValueKind.NON_INLINEABLE_ARGUMENT_FOR_INLINE_SUSPEND_PARAMETER ->
                        NonInlineableArgumentForInlineableSuspendParameter
                    else -> null
                }
            }

            if (!info.isSkippedOrRemapped) {
                val local = StackValue.local(codegen.frameMap.enterTemp(info.type), info.type)
                if (!isDefaultParameter) {
                    local.store(StackValue.onStack(info.typeOnStack), codegen.visitor)
                }
                if (info is CapturedParamInfo) {
                    info.remapValue = local
                    info.isSynthetic = true
                }
            }
        }
    }

    private fun leaveTemps() {
        invocationParamBuilder.listAllParams().asReversed().forEach { param ->
            if (!param.isSkippedOrRemapped || CapturedParamInfo.isSynthetic(param)) {
                codegen.frameMap.leaveTemp(param.type)
            }
        }
    }

    private fun rememberCapturedForDefaultLambda(defaultLambda: DefaultLambda) {
        if (!asFunctionInline) {
            for (captured in defaultLambda.capturedVars) {
                val info = invocationParamBuilder.addCapturedParam(captured, captured.fieldName, false)
                info.remapValue = StackValue.local(codegen.frameMap.enterTemp(info.type), info.type)
                info.isSynthetic = true
            }
        }
    }

    protected fun processDefaultMaskOrMethodHandler(value: StackValue, kind: ValueKind): Boolean {
        if (kind !== ValueKind.DEFAULT_MASK && kind !== ValueKind.METHOD_HANDLE_IN_DEFAULT) {
            return false
        }
        assert(value is StackValue.Constant) { "Additional default method argument should be constant, but $value" }
        val constantValue = (value as StackValue.Constant).value
        if (kind === ValueKind.DEFAULT_MASK) {
            assert(constantValue is Int) { "Mask should be of Integer type, but $constantValue" }
            maskValues.add(constantValue as Int)
            if (maskStartIndex == -1) {
                maskStartIndex = invocationParamBuilder.listAllParams().sumOf {
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

    private fun createInlineMethodNode(
        callDefault: Boolean,
        typeArguments: List<TypeParameterMarker>?,
        typeSystem: TypeSystemCommonBackendContext
    ): SMAPAndMethodNode {
        val intrinsic = generateInlineIntrinsic(state, functionDescriptor, jvmSignature.asmMethod, typeArguments, typeSystem)
        if (intrinsic != null) {
            return SMAPAndMethodNode(intrinsic, SMAP(listOf()))
        }
        sourceCompiler.inlineFunctionSignature(jvmSignature, callDefault)?.let { (containerId, asmMethod) ->
            val isMangled = requiresFunctionNameManglingForReturnType(functionDescriptor)
            return loadCompiledInlineFunction(containerId, asmMethod, functionDescriptor.isSuspend, isMangled, state)
        }
        return sourceCompiler.compileInlineFunction(jvmSignature, callDefault).apply {
            node.preprocessSuspendMarkers(forInline = true, keepFakeContinuation = false)
        }
    }

    companion object {
        internal fun loadCompiledInlineFunction(
            containerId: ClassId,
            asmMethod: Method,
            isSuspend: Boolean,
            isMangled: Boolean,
            state: GenerationState
        ): SMAPAndMethodNode {
            val containerType = AsmUtil.asmTypeByClassId(containerId)
            val bytes = state.inlineCache.classBytes.getOrPut(containerId) {
                findVirtualFile(state, containerId)?.contentsToByteArray()
                    ?: throw IllegalStateException("Couldn't find declaration file for $containerId")
            }
            val resultInCache = state.inlineCache.methodNodeById.getOrPut(MethodId(containerType.descriptor, asmMethod)) {
                getMethodNode(containerType, bytes, asmMethod.name, asmMethod.descriptor, isSuspend, isMangled)
            }
            return SMAPAndMethodNode(cloneMethodNode(resultInCache.node), resultInCache.classSMAP)
        }

        private fun getMethodNode(
            owner: Type,
            bytes: ByteArray,
            name: String,
            descriptor: String,
            isSuspend: Boolean,
            isMangled: Boolean
        ): SMAPAndMethodNode {
            getMethodNode(owner, bytes, name, descriptor, isSuspend)?.let { return it }
            if (isMangled) {
                // Compatibility with old inline class ABI versions.
                val dashIndex = name.indexOf('-')
                val nameWithoutManglingSuffix = if (dashIndex > 0) name.substring(0, dashIndex) else name
                if (nameWithoutManglingSuffix != name) {
                    getMethodNode(owner, bytes, nameWithoutManglingSuffix, descriptor, isSuspend)?.let { return it }
                }
                getMethodNode(owner, bytes, "$nameWithoutManglingSuffix-impl", descriptor, isSuspend)?.let { return it }
            }
            throw IllegalStateException("couldn't find inline method $owner.$name$descriptor")
        }

        // If an `inline suspend fun` has a state machine, it should have a `$$forInline` version without one.
        private fun getMethodNode(owner: Type, bytes: ByteArray, name: String, descriptor: String, isSuspend: Boolean) =
            (if (isSuspend) getMethodNode(bytes, name + FOR_INLINE_SUFFIX, descriptor, owner) else null)
                ?: getMethodNode(bytes, name, descriptor, owner)

        private fun StackValue.isCapturedInlineParameter(): Boolean {
            val field = if (this is StackValue.FieldForSharedVar) receiver else this
            return field is StackValue.Field && field.descriptor is ParameterDescriptor &&
                    InlineUtil.isInlineParameter(field.descriptor) &&
                    InlineUtil.isInline(field.descriptor.containingDeclaration)
        }
    }
}
