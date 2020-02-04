/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.stepping

import com.intellij.debugger.PositionManager
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.MethodFilter
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.impl.JvmSteppingCommandProvider
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.settings.DebuggerSettings
import com.intellij.psi.PsiElement
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.LocalVariable
import com.sun.jdi.Location
import com.sun.jdi.StackFrame
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.core.util.CodeInsightUtils
import org.jetbrains.kotlin.idea.core.util.getLineNumber
import org.jetbrains.kotlin.idea.debugger.*
import org.jetbrains.kotlin.idea.debugger.stepping.filter.KotlinSuspendCallStepOverFilter
import org.jetbrains.kotlin.idea.debugger.stepping.filter.LocationToken
import org.jetbrains.kotlin.idea.debugger.stepping.filter.StepOverCallerInfo
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.inline.InlineUtil
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
        if (sourcePosition.line < 0) return null
        return DebuggerSteppingHelper.createStepOutCommand(suspendContext, true)
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

fun getStepOverAction(
    location: Location, sourcePosition: SourcePosition,
    suspendContext: SuspendContextImpl, frameProxy: StackFrameProxyImpl
): KotlinStepAction {
    val stackFrame = frameProxy.safeStackFrame() ?: return KotlinStepAction.StepOver
    val method = location.safeMethod() ?: return KotlinStepAction.StepOver
    val token = LocationToken.from(stackFrame).takeIf { it.lineNumber > 0 } ?: return KotlinStepAction.StepOver

    val inlinedFunctionArgumentRanges = sourcePosition.collectInlineFunctionArgumentRanges()
    val positionManager = suspendContext.debugProcess.positionManager

    val tokensToSkip = mutableSetOf(token)

    for (candidate in method.allLineLocations() ?: emptyList()) {
        val candidateKotlinLineNumber = candidate.safeKotlinPreferredLineNumber()
        val candidateStackFrame = StackFrameForLocation(frameProxy.stackFrame, candidate)
        val candidateToken = LocationToken.from(candidateStackFrame)

        val isAcceptable = candidateToken.lineNumber >= 0
                && candidateToken.lineNumber != token.lineNumber
                && inlinedFunctionArgumentRanges.none { range -> range.contains(candidateKotlinLineNumber) }
                && candidateToken.inlineVariables.none { it !in token.inlineVariables }
                && !isInlineFunctionFromLibrary(positionManager, candidate, candidateToken)

        if (!isAcceptable) {
            tokensToSkip += candidateToken
        }
    }

    return KotlinStepAction.StepOverInlined(tokensToSkip, StepOverCallerInfo.from(location))
}

private fun isInlineFunctionFromLibrary(positionManager: PositionManager, location: Location, token: LocationToken): Boolean {
    if (token.inlineVariables.isEmpty()) {
        return false
    }

    val debuggerSettings = DebuggerSettings.getInstance()
    if (!debuggerSettings.TRACING_FILTERS_ENABLED) {
        return false
    }

    tailrec fun getDeclarationName(element: PsiElement?): FqName? {
        val declaration = element?.getNonStrictParentOfType<KtDeclaration>() ?: return null
        declaration.getKotlinFqName()?.let { return it }
        return getDeclarationName(declaration.parent)
    }

    val fqn = runReadAction {
        val element = positionManager.getSourcePosition(location)?.elementAt
        getDeclarationName(element)?.takeIf { !it.isRoot }?.asString()
    } ?: return false

    for (filter in debuggerSettings.steppingFilters) {
        if (filter.isEnabled && filter.matches(fqn)) {
            return true
        }
    }

    return false
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

fun getStepOutAction(location: Location, frameProxy: StackFrameProxyImpl): KotlinStepAction {
    val stackFrame = frameProxy.safeStackFrame() ?: return KotlinStepAction.StepOut
    val method = location.safeMethod() ?: return KotlinStepAction.StepOut
    val token = LocationToken.from(stackFrame).takeIf { it.lineNumber > 0 } ?: return KotlinStepAction.StepOut
    if (token.inlineVariables.isEmpty()) {
        return KotlinStepAction.StepOut
    }

    val tokensToSkip = mutableSetOf(token)

    for (candidate in method.allLineLocations() ?: emptyList()) {
        val candidateStackFrame = StackFrameForLocation(frameProxy.stackFrame, candidate)
        val candidateToken = LocationToken.from(candidateStackFrame)

        val isAcceptable = candidateToken.lineNumber >= 0
                && candidateToken.lineNumber != token.lineNumber
                && token.inlineVariables.any { it !in candidateToken.inlineVariables }

        if (!isAcceptable) {
            tokensToSkip += candidateToken
        }
    }

    return KotlinStepAction.StepOverInlined(tokensToSkip, StepOverCallerInfo.from(location))
}