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
import com.sun.jdi.*
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.core.util.CodeInsightUtils
import org.jetbrains.kotlin.idea.debugger.*
import org.jetbrains.kotlin.idea.core.util.getLineNumber
import org.jetbrains.kotlin.idea.core.util.getLineStartOffset
import org.jetbrains.kotlin.idea.debugger.stepping.filter.KotlinSuspendCallStepOverFilter
import org.jetbrains.kotlin.idea.debugger.stepping.filter.LocationToken
import org.jetbrains.kotlin.idea.debugger.stepping.filter.StepOverCallerInfo
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
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
        val file = sourcePosition.elementAt.containingFile
        val location = suspendContext.debugProcess.invokeInManagerThread { suspendContext.frameProxy?.safeLocation() } ?: return null
        if (isInSuspendMethod(location) && !isOnSuspendReturnOrReenter(location) && !isLastLineLocationInMethod(location)) {
            return DebuggerSteppingHelper.createStepOverCommandWithCustomFilter(
                suspendContext, ignoreBreakpoints, KotlinSuspendCallStepOverFilter(sourcePosition.line, file, ignoreBreakpoints)
            )
        }

        return DebuggerSteppingHelper.createStepOverCommand(suspendContext, ignoreBreakpoints, sourcePosition)
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

        val inlineFunctions = findInlineFunctions(file, lineStartOffset)
        val inlinedArgument = getInlineArgumentIfAny(sourcePosition.elementAt)

        if (inlineFunctions.isEmpty() && inlinedArgument == null) return null

        return DebuggerSteppingHelper.createStepOutCommand(suspendContext, true, inlineFunctions, inlinedArgument)
    }
}

private operator fun PsiElement?.contains(element: PsiElement): Boolean {
    return this?.textRange?.contains(element.textRange) ?: false
}

private fun findInlinedFunctionArguments(sourcePosition: SourcePosition): List<KtFunction> {
    val args = mutableListOf<KtFunction>()

    for (call in findInlineFunctionCalls(sourcePosition)) {
        for (arg in call.valueArguments) {
            val expression = arg.getArgumentExpression()
            val functionExpression = (expression as? KtLambdaExpression)?.functionLiteral ?: expression
            args += functionExpression as? KtFunction ?: continue
        }
    }

    return args
}

private fun findInlineFunctions(file: KtFile, offset: Int): List<KtNamedFunction> {
    val elementAt = file.findElementAt(offset) ?: return emptyList()
    val containingFunction = elementAt.getParentOfType<KtNamedFunction>(false) ?: return emptyList()

    val descriptor = containingFunction.unsafeResolveToDescriptor()
    if (!InlineUtil.isInline(descriptor)) return emptyList()

    return DebuggerUtils.analyzeElementWithInline(containingFunction, false).filterIsInstance<KtNamedFunction>()
}

private fun findInlineFunctionCalls(sourcePosition: SourcePosition): List<KtCallExpression> {
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

fun getStepOverAction(location: Location, sourcePosition: SourcePosition, frameProxy: StackFrameProxyImpl): KotlinStepAction {
    val stackFrame = frameProxy.safeStackFrame() ?: return KotlinStepAction.StepOver
    val method = location.safeMethod() ?: return KotlinStepAction.StepOver
    val token = LocationToken.from(stackFrame).takeIf { it.lineNumber > 0 } ?: return KotlinStepAction.StepOver

    val inlinedFunctionArgumentRanges = sourcePosition.collectInlineFunctionArgumentRanges()

    val tokensToSkip = mutableSetOf(token)

    for (candidate in method.allLineLocations() ?: emptyList()) {
        val candidateKotlinLineNumber = candidate.safeKotlinPreferredLineNumber()
        val candidateStackFrame = StackFrameForLocation(frameProxy.stackFrame, candidate)
        val candidateToken = LocationToken.from(candidateStackFrame)

        val isAcceptable = candidateToken.lineNumber >= 0
                && candidateToken.lineNumber != token.lineNumber
                && inlinedFunctionArgumentRanges.none { range -> range.contains(candidateKotlinLineNumber) }
                && candidateToken.inlineVariables.none { it !in token.inlineVariables }

        if (!isAcceptable) {
            tokensToSkip += candidateToken
        }
    }

    return KotlinStepAction.StepOverInlined(tokensToSkip, StepOverCallerInfo.from(location))
}

private fun SourcePosition.collectInlineFunctionArgumentRanges(): List<IntRange> {
    return runReadAction {
        val ranges = mutableListOf<IntRange>()
        for (arg in findInlinedFunctionArguments(this)) {
            val range = arg.getLineRange() ?: continue
            if (range.count() > 1) {
                ranges += range
            }
        }
        return@runReadAction ranges
    }
}

private class StackFrameForLocation(private val original: StackFrame, private val location: Location) : StackFrame by original {
    override fun location() = location

    override fun visibleVariables(): List<LocalVariable> {
        return location.method()?.variables()?.filter { it.isVisible(this) } ?: throw AbsentInformationException()
    }

    override fun visibleVariableByName(name: String?): LocalVariable {
        return location.method()?.variablesByName(name)?.firstOrNull { it.isVisible(this) } ?: throw AbsentInformationException()
    }
}

private fun KtElement.getLineRange(): IntRange? {
    val startLineNumber = getLineNumber(true)
    val endLineNumber = getLineNumber(false)
    if (startLineNumber > endLineNumber) {
        return null
    }

    return startLineNumber..endLineNumber
}

fun getStepOutAction(
    location: Location,
    suspendContext: SuspendContextImpl,
    inlineFunctions: List<KtNamedFunction>,
    inlinedArgument: KtFunctionLiteral?
): KotlinStepAction {
    val computedReferenceType = location.declaringType() ?: return KotlinStepAction.StepOut

    val locations = computedReferenceType.safeAllLineLocations()
    val nextLineLocations = locations
        .dropWhile { it != location }
        .drop(1)
        .filter { it.method() == location.method() }
        .dropWhile { it.lineNumber() == location.lineNumber() }

    if (inlineFunctions.isNotEmpty()) {
        val position = suspendContext.getXPositionForStepOutFromInlineFunction(nextLineLocations, inlineFunctions)
        return position?.let { KotlinStepAction.RunToCursor(it) } ?: KotlinStepAction.StepOver
    }

    if (inlinedArgument != null) {
        val position = suspendContext.getXPositionForStepOutFromInlinedArgument(nextLineLocations, inlinedArgument)
        return position?.let { KotlinStepAction.RunToCursor(it) } ?: KotlinStepAction.StepOver
    }

    return KotlinStepAction.StepOver
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

private fun getInlineArgumentIfAny(elementAt: PsiElement?): KtFunctionLiteral? {
    val functionLiteralExpression = elementAt?.getParentOfType<KtLambdaExpression>(false) ?: return null

    val context = functionLiteralExpression.analyze(BodyResolveMode.PARTIAL)
    if (!InlineUtil.isInlinedArgument(functionLiteralExpression.functionLiteral, context, false)) return null

    return functionLiteralExpression.functionLiteral
}