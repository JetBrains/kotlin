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
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.impl.JvmSteppingCommandProvider
import com.intellij.psi.PsiElement
import com.intellij.xdebugger.impl.XSourcePositionImpl
import com.sun.jdi.AbsentInformationException
import com.sun.jdi.Location
import org.jetbrains.annotations.TestOnly
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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class KotlinSteppingCommandProvider: JvmSteppingCommandProvider() {
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

    fun getStepOverCommand(suspendContext: SuspendContextImpl, ignoreBreakpoints: Boolean, sourcePosition: SourcePosition): DebugProcessImpl.ResumeCommand? {
        val file = sourcePosition.file as? KtFile ?: return null
        if (sourcePosition.line < 0) return null

        val containingFunction = sourcePosition.elementAt.parents.firstOrNull { it is KtNamedFunction && !it.isLocal } ?: return null

        val startLineNumber = containingFunction.getLineNumber(true)
        val endLineNumber = containingFunction.getLineNumber(false)
        if (startLineNumber > endLineNumber) return null

        val linesRange = startLineNumber + 1..endLineNumber + 1

        val inlineArgumentsToSkip = getElementsToSkip(containingFunction as KtNamedFunction, sourcePosition) ?: return null

        val additionalElementsToSkip = sourcePosition.elementAt.getAdditionalElementsToSkip()

        return DebuggerSteppingHelper.createStepOverCommand(suspendContext, ignoreBreakpoints, file, linesRange, inlineArgumentsToSkip, additionalElementsToSkip)
    }

    private fun getElementsToSkip(containingFunction: KtNamedFunction, sourcePosition: SourcePosition): List<KtFunction>? {
        val inlineFunctionCalls = getInlineFunctionCallsIfAny(sourcePosition)
        if (!inlineFunctionCalls.isEmpty()) {
            val inlineArguments = getInlineArgumentsIfAny(inlineFunctionCalls)
            if (inlineArguments.isNotEmpty() && inlineArguments.all { !it.shouldNotUseStepOver(sourcePosition.elementAt) }) {
                return inlineArguments
            }

            if (inlineArguments.isEmpty() && inlineFunctionCalls.all { !it.shouldNotUseStepOver(sourcePosition.elementAt) }) {
                return emptyList()
            }
        }

        if (InlineUtil.isInline(containingFunction.resolveToDescriptor())) {
            return emptyList()
        }

        return null
    }

    private fun PsiElement.getAdditionalElementsToSkip(): List<PsiElement> {
        val result = arrayListOf<PsiElement>()
        val ifParent = getParentOfType<KtIfExpression>(false)
        if (ifParent != null) {
            if (ifParent.then.contains(this)) {
                ifParent.elseKeyword?.let { result.add(it) }
                ifParent.`else`?.let { result.add(it) }
            }
        }
        val tryParent = getParentOfType<KtTryExpression>(false)
        if (tryParent != null) {
            val catchClause = getParentOfType<KtCatchClause>(false)
            if (catchClause != null) {
                result.addAll(tryParent.catchClauses.filter { it != catchClause })
            }
        }

        val whenEntry = getParentOfType<KtWhenEntry>(false)
        if (whenEntry != null) {
            if (whenEntry.expression.contains(this)) {
                val whenParent = whenEntry.getParentOfType<KtWhenExpression>(false)
                if (whenParent != null) {
                    result.addAll(whenParent.entries.filter { it != whenEntry })
                }
            }
        }

        return result
    }

    private fun PsiElement.shouldNotUseStepOver(elementAt: PsiElement): Boolean {
        val ifParent = getParentOfType<KtIfExpression>(false)
        if (ifParent != null) {
            // if (inlineFunCall()) {...}
            if (ifParent.condition.contains(this)) {
                return true
            }

            /*
            <caret>if (...) inlineFunCall()
                       else inlineFunCall()
             */
            val ifParentElementAt = elementAt.getParentOfType<KtIfExpression>(false)
            if (ifParentElementAt == null) {
                if (ifParent.then.contains(this)) {
                    return true
                }
                if (ifParent.`else`.contains(this)) {
                    return true
                }
            }
        }

        val tryParent = getParentOfType<KtTryExpression>(false)
        if (tryParent != null) {
            /* try { inlineFunCall() }
               catch()...
             */
            if (tryParent.tryBlock.contains(this)) {
                return true
            }
        }

        val whenEntry = getParentOfType<KtWhenEntry>(false)
        if (whenEntry != null) {
            // <caret>inlineFunCall -> ...
            if (whenEntry.conditions.any { it.contains(this) } ) {
                return true
            }

            // <caret>1 == 2 -> inlineFunCall()
            if (whenEntry.expression.contains(this)) {
                val parentEntryElementAt = elementAt.getParentOfType<KtWhenEntry>(false) ?: return true
                return parentEntryElementAt == whenEntry &&
                        whenEntry.conditions.any { it.contains(elementAt) }
            }
        }

        val whileParent = getParentOfType<KtWhileExpression>(false)
        if (whileParent != null) {
            // while (inlineFunCall()) {...}
            if (whileParent.condition.contains(this)) {
                return true
            }

            // last statement in while
            return (whileParent.body as? KtBlockExpression)?.statements?.lastOrNull()?.getLineNumber() == elementAt.getLineNumber()
        }

        return false
    }

    private fun PsiElement?.contains(element: PsiElement): Boolean {
        return this?.textRange?.contains(element.textRange) ?: false
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
                    .map { getArgumentExpression(it)  }
                    .filterIsInstance<KtFunction>()
        }
    }

    private fun getArgumentExpression(it: ValueArgument) = (it.getArgumentExpression() as? KtLambdaExpression)?.functionLiteral ?: it.getArgumentExpression()

    private fun getInlineFunctionCallsIfAny(sourcePosition: SourcePosition): List<KtCallExpression> {
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

        fun isInlineCall(expr: KtExpression): Boolean {
            val context = expr.analyze(BodyResolveMode.PARTIAL)
            val resolvedCall = expr.getResolvedCall(context) ?: return false
            return InlineUtil.isInline(resolvedCall.resultingDescriptor)
        }

        val allInlineFunctionCalls = CodeInsightUtils.
                findElementsOfClassInRange(file, start, end, KtExpression::class.java)
                .map { KtPsiUtil.getParentCallIfPresent(it as KtExpression) }
                .filterIsInstance<KtCallExpression>()
                .filter { isInlineCall(it) }
                .toSet()

        // It is necessary to check range because of multiline assign
        var linesRange = lineNumber..lineNumber
        return allInlineFunctionCalls.filter {
            val shouldInclude = it.getLineNumber() in linesRange
            if (shouldInclude) {
                linesRange = Math.min(linesRange.start, it.getLineNumber())..Math.max(linesRange.endInclusive, it.getLineNumber(false))
            }
            shouldInclude
        }
    }
}

sealed class Action(val position: XSourcePositionImpl?) {
    class STEP_OVER: Action(null)
    class STEP_OUT: Action(null)
    class RUN_TO_CURSOR(position: XSourcePositionImpl): Action(position)

    fun createCommand(
            debugProcess: DebugProcessImpl,
            suspendContext: SuspendContextImpl,
            ignoreBreakpoints: Boolean
    ): DebugProcessImpl.ResumeCommand? {
        return when (this) {
            is Action.RUN_TO_CURSOR -> {
                runReadAction {
                    debugProcess.createRunToCursorCommand(suspendContext, position!!, ignoreBreakpoints)
                }
            }
            is Action.STEP_OUT -> debugProcess.createStepOutCommand(suspendContext)
            is Action.STEP_OVER -> debugProcess.createStepOverCommand(suspendContext, ignoreBreakpoints)
        }
    }
}

fun getStepOverPosition(
        location: Location,
        file: KtFile,
        range: IntRange,
        inlinedArguments: List<KtElement>,
        elementsToSkip: List<PsiElement>
): Action {
    val computedReferenceType = location.declaringType() ?: return Action.STEP_OVER()

    fun isLocationSuitable(nextLocation: Location): Boolean {
        if (nextLocation.method() != location.method() || nextLocation.lineNumber() !in range) {
            return false
        }

        return try {
            nextLocation.sourceName("Kotlin") == file.name
        }
        catch(e: AbsentInformationException) {
            return true
        }
    }

    val locations = computedReferenceType.allLineLocations()
            .dropWhile { it != location }
            .drop(1)
            .filter { isLocationSuitable(it) }
            .dropWhile { it.lineNumber() == location.lineNumber() }

    for (locationAtLine in locations) {
        val xPosition = runReadAction l@ {
            val lineNumber = locationAtLine.lineNumber()
            val lineStartOffset = file.getLineStartOffset(lineNumber - 1) ?: return@l null
            if (inlinedArguments.any { it.textRange.contains(lineStartOffset) }) return@l null
            if (elementsToSkip.any { it.textRange.contains(lineStartOffset) }) return@l null

            if (locationAtLine.lineNumber() == location.lineNumber()) return@l null

            val elementAt = file.findElementAt(lineStartOffset) ?: return@l null
            XSourcePositionImpl.createByElement(elementAt)
        }

        if (xPosition != null) {
            return Action.RUN_TO_CURSOR(xPosition)
        }
    }

    if (locations.isNotEmpty()) {
        return Action.STEP_OUT()
    }

    return Action.STEP_OVER()
}

fun getStepOutPosition(
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

private fun getInlineArgumentIfAny(elementAt: PsiElement?): KtFunctionLiteral? {
    val functionLiteralExpression = elementAt?.getParentOfType<KtLambdaExpression>(false) ?: return null

    val context = functionLiteralExpression.analyze(BodyResolveMode.PARTIAL)
    if (!InlineUtil.isInlinedArgument(functionLiteralExpression.functionLiteral, context, false)) return null

    return functionLiteralExpression.functionLiteral
}
