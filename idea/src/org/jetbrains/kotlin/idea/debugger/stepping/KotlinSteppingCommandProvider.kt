/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.debugger.DebuggerUtils
import org.jetbrains.kotlin.idea.refactoring.getLineEndOffset
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.idea.refactoring.getLineStartOffset
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCallImpl
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

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
            sourcePosition: SourcePosition): DebugProcessImpl.ResumeCommand? {
        val kotlinSourcePosition = KotlinSourcePosition.create(sourcePosition) ?: return null

        if (!isSpecialStepOverNeeded(kotlinSourcePosition)) return null

        return DebuggerSteppingHelper.createStepOverCommand(suspendContext, ignoreBreakpoints, kotlinSourcePosition)
    }

    data class KotlinSourcePosition(val file: KtFile, val function: KtNamedFunction,
                                    val linesRange: IntRange, val sourcePosition: SourcePosition) {
        companion object {
            fun create(sourcePosition: SourcePosition): KotlinSourcePosition? {
                val file = sourcePosition.file as? KtFile ?: return null
                if (sourcePosition.line < 0) return null

                val containingFunction = sourcePosition.elementAt.parents
                                                 .filterIsInstance<KtNamedFunction>()
                                                 .firstOrNull { !it.isLocal } ?: return null

                val startLineNumber = containingFunction.getLineNumber(true) + 1
                val endLineNumber = containingFunction.getLineNumber(false) + 1
                if (startLineNumber > endLineNumber) return null

                val linesRange = startLineNumber..endLineNumber

                return KotlinSourcePosition(file, containingFunction, linesRange, sourcePosition)
            }
        }
    }

    private fun isSpecialStepOverNeeded(kotlinSourcePosition: KotlinSourcePosition): Boolean {
        val sourcePosition = kotlinSourcePosition.sourcePosition

        val hasInlineCallsOnLine = getInlineFunctionCallsIfAny(sourcePosition).isNotEmpty()
        if (hasInlineCallsOnLine) {
            return true
        }

        // Step over calls to lambda arguments in inline function while execution is already in that function
        val containingFunctionDescriptor = kotlinSourcePosition.function.resolveToDescriptor()
        if (InlineUtil.isInline(containingFunctionDescriptor)) {
            val inlineArgumentsCallsIfAny = getInlineArgumentsCallsIfAny(sourcePosition, containingFunctionDescriptor)
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

private fun PsiElement?.contains(element: PsiElement): Boolean {
    return this?.textRange?.contains(element.textRange) ?: false
}

private fun getInlineCallFunctionArgumentsIfAny(sourcePosition: SourcePosition): List<KtFunction> {
    val inlineFunctionCalls = getInlineFunctionCallsIfAny(sourcePosition)
    return getInlineArgumentsIfAny(inlineFunctionCalls)
}

private fun getInlineFunctionsIfAny(file: KtFile, offset: Int): List<KtNamedFunction> {
    val elementAt = file.findElementAt(offset) ?: return emptyList()
    val containingFunction = elementAt.getParentOfType<KtNamedFunction>(false) ?: return emptyList()

    val descriptor = containingFunction.resolveToDescriptor()
    if (!InlineUtil.isInline(descriptor)) return emptyList()

    val inlineFunctionsCalls = DebuggerUtils.analyzeElementWithInline(
            containingFunction.getResolutionFacade(),
            containingFunction.analyzeFully(),
            containingFunction,
            false
    ).filterIsInstance<KtNamedFunction>()

    return inlineFunctionsCalls
}

private fun getInlineArgumentsIfAny(inlineFunctionCalls: List<KtCallExpression>): List<KtFunction> {
    return inlineFunctionCalls.flatMap {
        it.valueArguments
                .map { getArgumentExpression(it) }
                .filterIsInstance<KtFunction>()
    }
}

private fun getArgumentExpression(it: ValueArgument) = (it.getArgumentExpression() as? KtLambdaExpression)?.functionLiteral ?: it.getArgumentExpression()

private fun getInlineArgumentsCallsIfAny(sourcePosition: SourcePosition, declarationDescriptor: DeclarationDescriptor): List<KtCallExpression>? {
    if (declarationDescriptor !is CallableDescriptor) return null

    val valueParameters = declarationDescriptor.valueParameters.filter { it.type.isFunctionType }.toSet()

    if (valueParameters.isEmpty()) {
        return null
    }

    fun isCallOfArgument(ktCallExpression: KtCallExpression): Boolean {
        val context = ktCallExpression.analyze(BodyResolveMode.PARTIAL)
        val resolvedCall = ktCallExpression.getResolvedCall(context) ?: return false

        if (resolvedCall !is VariableAsFunctionResolvedCallImpl) return false
        val candidateDescriptor = resolvedCall.variableCall.candidateDescriptor

        return candidateDescriptor in valueParameters
    }

    return findCallsOnPosition(sourcePosition, ::isCallOfArgument)
}

private fun getInlineFunctionCallsIfAny(sourcePosition: SourcePosition): List<KtCallExpression> {
    fun isInlineCall(expr: KtCallExpression): Boolean {
        val context = expr.analyze(BodyResolveMode.PARTIAL)
        val resolvedCall = expr.getResolvedCall(context) ?: return false
        return InlineUtil.isInline(resolvedCall.resultingDescriptor)
    }

    return findCallsOnPosition(sourcePosition, ::isInlineCall)
}

private fun findCallsOnPosition(sourcePosition: SourcePosition, filter: (KtCallExpression) -> Boolean): List<KtCallExpression> {
    val file = sourcePosition.file as? KtFile ?: return emptyList()
    val lineNumber = sourcePosition.line
    var elementAt = sourcePosition.elementAt

    var startOffset = file.getLineStartOffset(lineNumber) ?: elementAt.startOffset
    val endOffset = file.getLineEndOffset(lineNumber) ?: elementAt.endOffset

    var topMostElement: PsiElement? = null
    while (topMostElement !is KtElement && startOffset < endOffset) {
        elementAt = file.findElementAt(startOffset)
        if (elementAt != null) {
            topMostElement = CodeInsightUtils.getTopmostElementAtOffset(elementAt, startOffset)
        }
        startOffset++
    }

    if (topMostElement !is KtElement) return emptyList()

    val start = topMostElement.startOffset
    val end = topMostElement.endOffset

    val allFilteredCalls = CodeInsightUtils.
            findElementsOfClassInRange(file, start, end, KtExpression::class.java)
            .map { KtPsiUtil.getParentCallIfPresent(it as KtExpression) }
            .filterIsInstance<KtCallExpression>()
            .filter { filter(it) }
            .toSet()

    // It is necessary to check range because of multiline assign
    var linesRange = lineNumber..lineNumber
    return allFilteredCalls.filter {
        val shouldInclude = it.getLineNumber() in linesRange
        if (shouldInclude) {
            linesRange = Math.min(linesRange.start, it.getLineNumber())..Math.max(linesRange.endInclusive, it.getLineNumber(false))
        }
        shouldInclude
    }
}


sealed class Action(val position: XSourcePositionImpl? = null,
                    val lineNumber: Int? = null,
                    val stepOverLines: Set<Int>? = null,
                    val inlineRangeVariables: List<LocalVariable>? = null) {
    class STEP_OVER : Action()
    class STEP_OUT : Action()
    class RUN_TO_CURSOR(position: XSourcePositionImpl) : Action(position)
    class STEP_OVER_INLINED(lineNumber: Int, stepOverLines: Set<Int>, inlineVariables: List<LocalVariable>) : Action(
            lineNumber = lineNumber, stepOverLines = stepOverLines, inlineRangeVariables = inlineVariables)

    fun apply(debugProcess: DebugProcessImpl,
              suspendContext: SuspendContextImpl,
              ignoreBreakpoints: Boolean) {
        when (this) {
            is Action.RUN_TO_CURSOR -> {
                runReadAction {
                    debugProcess.createRunToCursorCommand(suspendContext, position!!, ignoreBreakpoints)
                }.contextAction()
            }
            is Action.STEP_OUT -> debugProcess.createStepOutCommand(suspendContext).contextAction()
            is Action.STEP_OVER -> debugProcess.createStepOverCommand(suspendContext, ignoreBreakpoints).contextAction()
            is Action.STEP_OVER_INLINED -> KotlinStepActionFactory(debugProcess).createKotlinStepOverInlineAction(
                    KotlinStepOverInlineFilter(stepOverLines!!, lineNumber ?: -1, inlineRangeVariables!!)).contextAction(suspendContext)
        }
    }
}

interface KotlinMethodFilter: MethodFilter {
    fun locationMatches(context: SuspendContextImpl, location: Location): Boolean
}

fun getStepOverAction(
        location: Location,
        kotlinSourcePosition: KotlinSteppingCommandProvider.KotlinSourcePosition,
        frameProxy: StackFrameProxyImpl
): Action {
    val inlineArgumentsToSkip = runReadAction {
        getInlineCallFunctionArgumentsIfAny(kotlinSourcePosition.sourcePosition)
    }

    return getStepOverAction(location, kotlinSourcePosition.file, kotlinSourcePosition.linesRange,
                             inlineArgumentsToSkip, frameProxy)
}

fun getStepOverAction(
        location: Location,
        file: KtFile,
        range: IntRange,
        inlineFunctionArguments: List<KtElement>,
        frameProxy: StackFrameProxyImpl
): Action {
    val computedReferenceType = location.declaringType() ?: return Action.STEP_OVER()

    fun isLocationSuitable(nextLocation: Location): Boolean {
        if (nextLocation.method() != location.method() || nextLocation.lineNumber() !in range) {
            return false
        }

        return try {
            nextLocation.sourceName(KOTLIN_STRATA_NAME) == file.name
        }
        catch(e: AbsentInformationException) {
            return true
        }
    }

    fun isBackEdgeLocation(): Boolean {
        val previousSuitableLocation = computedReferenceType.allLineLocations().reversed()
                .dropWhile { it != location }
                .drop(1)
                .filter(::isLocationSuitable)
                .dropWhile { it.lineNumber() == location.lineNumber() }
                .firstOrNull()

        return previousSuitableLocation != null && previousSuitableLocation.lineNumber() > location.lineNumber()
    }

    val patchedLocation = if (isBackEdgeLocation()) {
        // Pretend we had already did a backing step
        computedReferenceType.allLineLocations()
                .filter(::isLocationSuitable)
                .first { it.lineNumber() == location.lineNumber() }
    }
    else {
        location
    }

    val lambdaArgumentRanges = runReadAction {
        inlineFunctionArguments.filterIsInstance<KtElement>().map {
            val startLineNumber = it.getLineNumber(true) + 1
            val endLineNumber = it.getLineNumber(false) + 1

            startLineNumber..endLineNumber
        }
    }

    val inlineRangeVariables = getInlineRangeLocalVariables(frameProxy)

    // Try to find the range of inlined lines:
    // - Lines from other files and from functions that are not in range of current one are definitely inlined
    // - Lines in function arguments of inlined functions are inlined too as we found them starting from the position of inlined call.
    //
    // This heuristic doesn't work for DEX, because of missing strata information (https://code.google.com/p/android/issues/detail?id=82972)
    //
    // It also thinks that too many lines are inlined when there's a call of function argument or other
    // inline function in last statement of inline function. The list of inlineRangeVariables is used to overcome it.
    val probablyInlinedLocations = computedReferenceType.allLineLocations()
            .dropWhile { it != patchedLocation }
            .drop(1)
            .dropWhile { it.lineNumber() == patchedLocation.lineNumber() }
            .takeWhile { locationAtLine ->
                !isLocationSuitable(locationAtLine) || lambdaArgumentRanges.any { locationAtLine.lineNumber() in it }
            }
            .dropWhile { it.lineNumber() == patchedLocation.lineNumber() }

    if (!probablyInlinedLocations.isEmpty()) {
        return Action.STEP_OVER_INLINED(patchedLocation.lineNumber(), probablyInlinedLocations.map { it.lineNumber() }.toSet(), inlineRangeVariables)
    }

    return Action.STEP_OVER()
}

fun getStepOutAction(
        location: Location,
        suspendContext: SuspendContextImpl,
        inlineFunctions: List<KtNamedFunction>,
        inlinedArgument: KtFunctionLiteral?
): Action {
    val computedReferenceType = location.declaringType() ?: return Action.STEP_OUT()

    val locations = computedReferenceType.allLineLocations()
    val nextLineLocations = locations
            .dropWhile { it != location }
            .drop(1)
            .filter { it.method() == location.method() }
            .dropWhile { it.lineNumber() == location.lineNumber() }

    if (inlineFunctions.isNotEmpty()) {
        val position = suspendContext.getXPositionForStepOutFromInlineFunction(nextLineLocations, inlineFunctions)
        return position?.let { Action.RUN_TO_CURSOR(it) } ?: Action.STEP_OVER()
    }

    if (inlinedArgument != null) {
        val position = suspendContext.getXPositionForStepOutFromInlinedArgument(nextLineLocations, inlinedArgument)
        return position?.let { Action.RUN_TO_CURSOR(it) } ?: Action.STEP_OVER()
    }

    return Action.STEP_OVER()
}

private fun SuspendContextImpl.getXPositionForStepOutFromInlineFunction(
        locations: List<Location>,
        inlineFunctionsToSkip: List<KtNamedFunction>
): XSourcePositionImpl? {
    return getNextPositionWithFilter(locations) {
        offset, elementAt ->
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
    return getNextPositionWithFilter(locations) {
        offset, elementAt ->
        inlinedArgumentToSkip.textRange.contains(offset)
    }
}

private fun SuspendContextImpl.getNextPositionWithFilter(
        locations: List<Location>,
        skip: (Int, PsiElement) -> Boolean
): XSourcePositionImpl? {
    for (location in locations) {
        val position = runReadAction l@ {
            val sourcePosition = try {
                this.debugProcess.positionManager.getSourcePosition(location)
            }
                                 catch(e: NoDataException) {
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
    return stackFrame.visibleVariables()
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
