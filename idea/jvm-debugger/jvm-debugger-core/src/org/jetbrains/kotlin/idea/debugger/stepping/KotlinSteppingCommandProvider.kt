/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.stepping

import com.intellij.debugger.NoDataException
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.MethodFilter
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.impl.JvmSteppingCommandProvider
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.psi.PsiElement
import com.intellij.xdebugger.impl.XSourcePositionImpl
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.LocalVariable
import com.sun.jdi.Location
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.codegen.inline.KOTLIN_STRATA_NAME
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.core.util.CodeInsightUtils
import org.jetbrains.kotlin.idea.debugger.*
import org.jetbrains.kotlin.idea.core.util.getLineNumber
import org.jetbrains.kotlin.idea.core.util.getLineStartOffset
import org.jetbrains.kotlin.idea.debugger.stepping.filter.KotlinStepOverInlineFilter
import org.jetbrains.kotlin.idea.debugger.stepping.filter.KotlinSuspendCallStepOverFilter
import org.jetbrains.kotlin.idea.debugger.stepping.filter.StepOverFilterData
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import kotlin.math.max
import kotlin.math.min

class KotlinSteppingCommandProvider : JvmSteppingCommandProvider() {
    override fun getStepOverCommand(
        suspendContext: SuspendContextImpl?,
        ignoreBreakpoints: Boolean,
        stepSize: Int
    ): DebugProcessImpl.ResumeCommand? {
        if (suspendContext == null || suspendContext.isResumed) return null

        val sourcePosition = suspendContext.debugProcess.debuggerContext.sourcePosition ?: return null
        return getStepOverCommand(suspendContext, ignoreBreakpoints, sourcePosition)
    }

    @TestOnly
    fun getStepOverCommand(
        suspendContext: SuspendContextImpl,
        ignoreBreakpoints: Boolean,
        debuggerContext: DebuggerContextImpl
    ): DebugProcessImpl.ResumeCommand? {
        return getStepOverCommand(suspendContext, ignoreBreakpoints, debuggerContext.sourcePosition)
    }

    private fun getStepOverCommand(
        suspendContext: SuspendContextImpl,
        ignoreBreakpoints: Boolean,
        sourcePosition: SourcePosition
    ): DebugProcessImpl.ResumeCommand? {
        val kotlinSourcePosition = KotlinSourcePosition.create(sourcePosition) ?: return null

        if (isSpecialStepOverNeeded(kotlinSourcePosition)) {
            return DebuggerSteppingHelper.createStepOverCommand(suspendContext, ignoreBreakpoints, kotlinSourcePosition)
        }

        val file = sourcePosition.elementAt.containingFile
        val location = suspendContext.debugProcess.invokeInManagerThread { suspendContext.frameProxy?.safeLocation() } ?: return null
        if (isInSuspendMethod(location) && !isOnSuspendReturnOrReenter(location) && !isLastLineLocationInMethod(location)) {
            return DebuggerSteppingHelper.createStepOverCommandWithCustomFilter(
                suspendContext, ignoreBreakpoints, KotlinSuspendCallStepOverFilter(sourcePosition.line, file, ignoreBreakpoints)
            )
        }

        return null
    }

    private fun isSpecialStepOverNeeded(kotlinSourcePosition: KotlinSourcePosition): Boolean {
        val sourcePosition = kotlinSourcePosition.sourcePosition

        val hasInlineCallsOnLine = getInlineFunctionCallsIfAny(sourcePosition).isNotEmpty()
        if (hasInlineCallsOnLine) {
            return true
        }

        // Step over calls to lambda arguments in inline function while execution is already in that function
        val containingDescriptor = kotlinSourcePosition.declaration.resolveToDescriptorIfAny()
        if (containingDescriptor != null && InlineUtil.isInline(containingDescriptor)) {
            val inlineArgumentsCallsIfAny = getInlineArgumentsCallsIfAny(sourcePosition, containingDescriptor)
            if (inlineArgumentsCallsIfAny != null && inlineArgumentsCallsIfAny.isNotEmpty()) {
                return true
            }
        }

        return false
    }

    @TestOnly
    fun getStepOutCommand(suspendContext: SuspendContextImpl, debugContext: DebuggerContextImpl): DebugProcessImpl.ResumeCommand? {
        return getStepOutCommand(suspendContext, debugContext.sourcePosition)
    }

    override fun getStepOutCommand(suspendContext: SuspendContextImpl?, stepSize: Int): DebugProcessImpl.ResumeCommand? {
        if (suspendContext == null || suspendContext.isResumed) return null

        val sourcePosition = suspendContext.debugProcess.debuggerContext.sourcePosition ?: return null
        return getStepOutCommand(suspendContext, sourcePosition)
    }

    private fun getStepOutCommand(suspendContext: SuspendContextImpl, sourcePosition: SourcePosition): DebugProcessImpl.ResumeCommand? {
        val file = sourcePosition.file as? KtFile ?: return null
        if (sourcePosition.line < 0) return null

        val lineStartOffset = file.getLineStartOffset(sourcePosition.line) ?: return null

        val inlineFunctions = getInlineFunctionsIfAny(file, lineStartOffset)
        val inlinedArgument = getInlineArgumentIfAny(sourcePosition.elementAt)

        if (inlineFunctions.isEmpty() && inlinedArgument == null) return null

        return DebuggerSteppingHelper.createStepOutCommand(suspendContext, true, inlineFunctions, inlinedArgument)
    }
}

private operator fun PsiElement?.contains(element: PsiElement): Boolean {
    return this?.textRange?.contains(element.textRange) ?: false
}

private fun getInlineCallFunctionArgumentsIfAny(sourcePosition: SourcePosition): List<KtFunction> {
    val inlineFunctionCalls = getInlineFunctionCallsIfAny(sourcePosition)
    return getInlineArgumentsIfAny(inlineFunctionCalls)
}

private fun getInlineFunctionsIfAny(file: KtFile, offset: Int): List<KtNamedFunction> {
    val elementAt = file.findElementAt(offset) ?: return emptyList()
    val containingFunction = elementAt.getParentOfType<KtNamedFunction>(false) ?: return emptyList()

    val descriptor = containingFunction.unsafeResolveToDescriptor()
    if (!InlineUtil.isInline(descriptor)) return emptyList()

    return DebuggerUtils.analyzeElementWithInline(containingFunction, false).filterIsInstance<KtNamedFunction>()
}

private fun getInlineArgumentsIfAny(inlineFunctionCalls: List<KtCallExpression>): List<KtFunction> {
    return inlineFunctionCalls.flatMap {
        it.valueArguments
            .map(::getArgumentExpression)
            .filterIsInstance<KtFunction>()
    }
}

private fun getArgumentExpression(it: ValueArgument) =
    (it.getArgumentExpression() as? KtLambdaExpression)?.functionLiteral ?: it.getArgumentExpression()

private fun getInlineArgumentsCallsIfAny(
    sourcePosition: SourcePosition,
    declarationDescriptor: DeclarationDescriptor
): List<KtCallExpression>? {
    if (declarationDescriptor !is CallableDescriptor) return null

    val valueParameters = declarationDescriptor.valueParameters.filter { it.type.isFunctionType }.toSet()

    if (valueParameters.isEmpty()) {
        return null
    }

    fun isCallOfArgument(ktCallExpression: KtCallExpression): Boolean {
        val resolvedCall = ktCallExpression.resolveToCall() as? VariableAsFunctionResolvedCall ?: return false

        val candidateDescriptor = resolvedCall.variableCall.candidateDescriptor

        return candidateDescriptor in valueParameters
    }

    return findCallsOnPosition(sourcePosition, ::isCallOfArgument)
}

private fun getInlineFunctionCallsIfAny(sourcePosition: SourcePosition): List<KtCallExpression> {
    fun isInlineCall(expr: KtCallExpression): Boolean {
        val resolvedCall = expr.resolveToCall() ?: return false
        return InlineUtil.isInline(resolvedCall.resultingDescriptor)
    }

    return findCallsOnPosition(sourcePosition, ::isInlineCall)
}

private fun findCallsOnPosition(sourcePosition: SourcePosition, filter: (KtCallExpression) -> Boolean): List<KtCallExpression> {
    val file = sourcePosition.file as? KtFile ?: return emptyList()
    val lineNumber = sourcePosition.line

    val lineElement = findElementAtLine(file, lineNumber)

    if (lineElement !is KtElement) {
        if (lineElement != null) {
            val call = findCallByEndToken(lineElement)
            if (call != null && filter(call)) {
                return listOf(call)
            }
        }

        return emptyList()
    }

    val start = lineElement.startOffset
    val end = lineElement.endOffset

    val allFilteredCalls = CodeInsightUtils.findElementsOfClassInRange(file, start, end, KtExpression::class.java)
        .map { KtPsiUtil.getParentCallIfPresent(it as KtExpression) }
        .filterIsInstance<KtCallExpression>()
        .filter { filter(it) }
        .toSet()

    // It is necessary to check range because of multiline assign
    var linesRange = lineNumber..lineNumber
    return allFilteredCalls.filter {
        val shouldInclude = it.getLineNumber() in linesRange
        if (shouldInclude) {
            linesRange = min(linesRange.first, it.getLineNumber())..max(linesRange.last, it.getLineNumber(false))
        }
        shouldInclude
    }
}

interface KotlinMethodFilter : MethodFilter {
    fun locationMatches(context: SuspendContextImpl, location: Location): Boolean
}

fun getStepOverAction(location: Location, kotlinSourcePosition: KotlinSourcePosition, frameProxy: StackFrameProxyImpl): KotlinStepAction {
    val inlineArgumentsToSkip = runReadAction {
        getInlineCallFunctionArgumentsIfAny(kotlinSourcePosition.sourcePosition)
    }

    return getStepOverAction(
        location, kotlinSourcePosition.file, kotlinSourcePosition.linesRange,
        inlineArgumentsToSkip, frameProxy
    )
}

fun getStepOverAction(
    location: Location,
    sourceFile: KtFile,
    range: IntRange,
    inlineFunctionArguments: List<KtElement>,
    frameProxy: StackFrameProxyImpl
): KotlinStepAction {
    location.declaringType() ?: return KotlinStepAction.StepOver()

    val methodLocations = location.method().safeAllLineLocations()
    if (methodLocations.isEmpty()) {
        return KotlinStepAction.StepOver()
    }

    fun isThisMethodLocation(nextLocation: Location): Boolean {
        if (nextLocation.method() != location.method()) {
            return false
        }

        val ktLineNumber = nextLocation.lineNumber()
        if (ktLineNumber !in range) {
            return false
        }

        return try {
            nextLocation.sourceName(KOTLIN_STRATA_NAME) == sourceFile.name
        } catch (e: AbsentInformationException) {
            true
        }
    }

    fun isBackEdgeLocation(): Boolean {
        val previousSuitableLocation = methodLocations.reversed()
            .asSequence()
            .dropWhile { it != location }
            .drop(1)
            .filter(::isThisMethodLocation)
            .dropWhile { it.lineNumber() == location.lineNumber() }
            .firstOrNull()

        return previousSuitableLocation != null && previousSuitableLocation.lineNumber() > location.lineNumber()
    }

    val patchedLocation = if (isBackEdgeLocation()) {
        // Pretend we had already done a backing step
        methodLocations
            .filter(::isThisMethodLocation)
            .firstOrNull { it.lineNumber() == location.lineNumber() } ?: location
    } else {
        location
    }

    val patchedLineNumber = patchedLocation.lineNumber()

    val lambdaArgumentRanges = runReadAction {
        inlineFunctionArguments.map {
            val startLineNumber = it.getLineNumber(true) + 1
            val endLineNumber = it.getLineNumber(false) + 1

            startLineNumber..endLineNumber
        }
    }

    val inlineRangeVariables = getInlineRangeLocalVariables(frameProxy)

    // Try to find the range for step over:
    // - Lines from other files and from functions that are not in range of current one are definitely inlined and should be stepped over.
    // - Lines in function arguments of inlined functions are inlined too as we found them starting from the position of inlined call.
    // - Current line locations should also be stepped over.
    //
    // We might erroneously extend this range too much when there's a call of function argument or other
    // inline function in last statement of inline function. The list of inlineRangeVariables will be used later to overcome it.
    val stepOverLocations = methodLocations
        .dropWhile { it != patchedLocation }
        .drop(1)
        .dropWhile { it.lineNumber() == patchedLineNumber }
        .takeWhile { loc ->
            !isThisMethodLocation(loc) || lambdaArgumentRanges.any { loc.lineNumber() in it } || loc.lineNumber() == patchedLineNumber
        }

    if (stepOverLocations.isNotEmpty()) {
        return KotlinStepAction.StepOverInlined(
            StepOverFilterData(
                patchedLineNumber,
                stepOverLocations.map { it.lineNumber() }.toSet(),
                inlineRangeVariables
            )
        )
    }

    return KotlinStepAction.StepOver()
}

fun getStepOutAction(
    location: Location,
    suspendContext: SuspendContextImpl,
    inlineFunctions: List<KtNamedFunction>,
    inlinedArgument: KtFunctionLiteral?
): KotlinStepAction {
    val computedReferenceType = location.declaringType() ?: return KotlinStepAction.StepOut()

    val locations = computedReferenceType.safeAllLineLocations()
    val nextLineLocations = locations
        .dropWhile { it != location }
        .drop(1)
        .filter { it.method() == location.method() }
        .dropWhile { it.lineNumber() == location.lineNumber() }

    if (inlineFunctions.isNotEmpty()) {
        val position = suspendContext.getXPositionForStepOutFromInlineFunction(nextLineLocations, inlineFunctions)
        return position?.let { KotlinStepAction.RunToCursor(it) } ?: KotlinStepAction.StepOver()
    }

    if (inlinedArgument != null) {
        val position = suspendContext.getXPositionForStepOutFromInlinedArgument(nextLineLocations, inlinedArgument)
        return position?.let { KotlinStepAction.RunToCursor(it) } ?: KotlinStepAction.StepOver()
    }

    return KotlinStepAction.StepOver()
}

private fun SuspendContextImpl.getXPositionForStepOutFromInlineFunction(
    locations: List<Location>,
    inlineFunctionsToSkip: List<KtNamedFunction>
): XSourcePositionImpl? {
    return getNextPositionWithFilter(locations) { offset, elementAt ->
        if (inlineFunctionsToSkip.any { it.textRange.contains(offset) }) {
            return@getNextPositionWithFilter true
        }

        getInlineArgumentIfAny(elementAt) != null
    }
}

private fun SuspendContextImpl.getXPositionForStepOutFromInlinedArgument(
    locations: List<Location>,
    inlinedArgumentToSkip: KtFunctionLiteral
): XSourcePositionImpl? {
    return getNextPositionWithFilter(locations) { offset, _ ->
        inlinedArgumentToSkip.textRange.contains(offset)
    }
}

private fun SuspendContextImpl.getNextPositionWithFilter(
    locations: List<Location>,
    skip: (Int, PsiElement) -> Boolean
): XSourcePositionImpl? {
    for (location in locations) {
        val position = runReadAction l@{
            val sourcePosition = try {
                this.debugProcess.positionManager.getSourcePosition(location)
            } catch (e: NoDataException) {
                null
            } ?: return@l null

            val file = sourcePosition.file as? KtFile ?: return@l null
            val elementAt = sourcePosition.elementAt ?: return@l null
            val currentLine = location.lineNumber() - 1
            val lineStartOffset = file.getLineStartOffset(currentLine) ?: return@l null
            if (skip(lineStartOffset, elementAt)) return@l null

            XSourcePositionImpl.createByElement(elementAt)
        }
        if (position != null) {
            return position
        }
    }

    return null
}

fun getInlineRangeLocalVariables(stackFrame: StackFrameProxyImpl): List<LocalVariable> {
    return stackFrame.safeVisibleVariables()
        .filter {
            val name = it.name()
            name.startsWith(JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION)
        }
        .map { it.variable }
}

private fun getInlineArgumentIfAny(elementAt: PsiElement?): KtFunctionLiteral? {
    val functionLiteralExpression = elementAt?.getParentOfType<KtLambdaExpression>(false) ?: return null

    val context = functionLiteralExpression.analyze(BodyResolveMode.PARTIAL)
    if (!InlineUtil.isInlinedArgument(functionLiteralExpression.functionLiteral, context, false)) return null

    return functionLiteralExpression.functionLiteral
}