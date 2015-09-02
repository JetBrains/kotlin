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

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.events.DebuggerCommandImpl
import com.intellij.debugger.impl.JvmSteppingCommandProvider
import com.intellij.util.concurrency.Semaphore
import com.intellij.xdebugger.impl.XSourcePositionImpl
import com.sun.jdi.Location
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.core.refactoring.getLineCount
import org.jetbrains.kotlin.idea.core.refactoring.getLineStartOffset
import org.jetbrains.kotlin.idea.util.DebuggerUtils
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

public class KotlinSteppingCommandProvider: JvmSteppingCommandProvider() {
    override fun getStepOverCommand(
            suspendContext: SuspendContextImpl?,
            ignoreBreakpoints: Boolean,
            stepSize: Int
    ): DebugProcessImpl.ResumeCommand? {
        if (suspendContext == null) return null

        val location = computeInManagerThread(suspendContext) {
            it.frameProxy?.location()
        } ?: return null

        val sourcePosition = suspendContext.debugProcess.positionManager.getSourcePosition(location) ?: return null
        val computedReferenceType = location.declaringType() ?: return null

        val file = sourcePosition.file as? JetFile ?: return null

        val inlinedArguments = getInlinedArgumentsIfAny(sourcePosition) ?: return null

        val locations = computedReferenceType.allLineLocations()
        val countOfLinesInFile = file.getLineCount()

        val nextLine = sourcePosition.line + 2 /* +1 - because of locations are counted from 1 and +1 - because we want next line */

        for (lineNumber in nextLine..countOfLinesInFile) {
            val locationAtLine = locations.firstOrNull { it.lineNumber() == lineNumber }
            if (locationAtLine != null) {
                val lineStartOffset = file.getLineStartOffset(lineNumber - 1) ?: continue
                if (inlinedArguments.any { it.textRange.contains(lineStartOffset) }) continue

                val elementAt = file.findElementAt(lineStartOffset)
                val xPosition = XSourcePositionImpl.createByElement(elementAt) ?: return null

                return suspendContext.debugProcess.createRunToCursorCommand(suspendContext, xPosition, ignoreBreakpoints)
            }
        }

        return null
    }

    override fun getStepOutCommand(suspendContext: SuspendContextImpl?, stepSize: Int): DebugProcessImpl.ResumeCommand? {
        if (suspendContext == null) return null

        val location = computeInManagerThread(suspendContext) {
            it.frameProxy?.location()
        } ?: return null

        val sourcePosition = suspendContext.debugProcess.positionManager.getSourcePosition(location) ?: return null
        val computedReferenceType = location.declaringType() ?: return null

        val locations = computedReferenceType.allLineLocations()

        val file = sourcePosition.file as? JetFile ?: return null
        val lineStartOffset = file.getLineStartOffset(sourcePosition.line) ?: return null
        val nextLineLocations = locations.dropWhile { it.lineNumber() != location.lineNumber() }.filter { it.method() == location.method() }

        val inlineFunction = getInlineFunctionsIfAny(file, lineStartOffset)
        if (inlineFunction != null) {
            val xPosition = suspendContext.getXPositionForStepOutFromInlineFunction(nextLineLocations, inlineFunction) ?: return null
            return suspendContext.debugProcess.createRunToCursorCommand(suspendContext, xPosition, true)
        }

        val inlinedArgument = getInlineArgumentIfAny(file, lineStartOffset)
        if (inlinedArgument != null) {
            val xPosition = suspendContext.getXPositionForStepOutFromInlinedArgument(nextLineLocations, inlinedArgument) ?: return null
            return suspendContext.debugProcess.createRunToCursorCommand(suspendContext, xPosition, true)
        }

        return null
    }

    private fun SuspendContextImpl.getXPositionForStepOutFromInlineFunction(
            locations: List<Location>,
            inlineFunctionsToSkip: List<JetNamedFunction>
    ): XSourcePositionImpl? {
        return getNextPositionWithFilter(locations) {
            file, offset ->
            if (inlineFunctionsToSkip.any { it.textRange.contains(offset) }) {
                return@getNextPositionWithFilter true
            }

            val inlinedArgument = getInlineArgumentIfAny(file, offset)
            inlinedArgument != null && inlinedArgument.textRange.contains(offset)
        }
    }

    private fun SuspendContextImpl.getXPositionForStepOutFromInlinedArgument(
            locations: List<Location>,
            inlinedArgumentToSkip: JetFunctionLiteral
    ): XSourcePositionImpl? {
        return getNextPositionWithFilter(locations) {
            file, offset ->
            inlinedArgumentToSkip.textRange.contains(offset)
        }
    }

    private fun SuspendContextImpl.getNextPositionWithFilter(
            locations: List<Location>,
            skip: (JetFile, Int) -> Boolean
    ): XSourcePositionImpl? {
        for (location in locations) {
            val file = this.debugProcess.positionManager.getSourcePosition(location)?.file as? JetFile ?: continue
            val currentLine = location.lineNumber() - 1
            val lineStartOffset = file.getLineStartOffset(currentLine) ?: continue
            if (skip(file, lineStartOffset)) continue

            val elementAt = file.findElementAt(lineStartOffset) ?: continue
            return XSourcePositionImpl.createByElement(elementAt)
        }

        return null
    }

    private fun getInlineFunctionsIfAny(file: JetFile, offset: Int): List<JetNamedFunction>? {
        val elementAt = file.findElementAt(offset) ?: return null
        val containingFunction = elementAt.getParentOfType<JetNamedFunction>(false) ?: return null

        val descriptor = containingFunction.resolveToDescriptor()
        if (!InlineUtil.isInline(descriptor)) return null

        val inlineFunctionsCalls = DebuggerUtils.analyzeElementWithInline(
                containingFunction.getResolutionFacade(),
                containingFunction.analyzeFully(),
                containingFunction,
                false
        ).filterIsInstance<JetNamedFunction>()

        return inlineFunctionsCalls
    }

    private fun getInlineArgumentIfAny(file: JetFile, offset: Int): JetFunctionLiteral? {
        val elementAt = file.findElementAt(offset) ?: return null
        val functionLiteralExpression = elementAt.getParentOfType<JetFunctionLiteralExpression>(false) ?: return null

        val context = functionLiteralExpression.analyze(BodyResolveMode.PARTIAL)
        if (!InlineUtil.isInlinedArgument(functionLiteralExpression.functionLiteral, context, false)) return null

        return functionLiteralExpression.functionLiteral
    }

    private fun isKotlinStrataAvailable(suspendContext: SuspendContextImpl): Boolean {
        val availableStrata = suspendContext.frameProxy?.location()?.declaringType()?.availableStrata() ?: return false
        return availableStrata.contains("Kotlin")
    }

    private fun <T: Any> computeInManagerThread(suspendContext: SuspendContextImpl, action: (SuspendContextImpl) -> T?): T? {
        val semaphore = Semaphore()
        semaphore.down()

        var result : T? = null
        val worker = object : DebuggerCommandImpl() {
            override fun action() {
                try {
                    if (isKotlinStrataAvailable(suspendContext)) {
                        result = action(suspendContext)
                    }
                }
                finally {
                    semaphore.up()
                }
            }
        }

        suspendContext.debugProcess.managerThread?.invoke(worker)

        for (i in 0..25) {
            if (semaphore.waitFor(20)) break
        }

        return result
    }

    private fun getInlinedArgumentsIfAny(sourcePosition: SourcePosition): List<JetFunction>? {
        val file = sourcePosition.file as? JetFile ?: return null
        val lineNumber = sourcePosition.line
        val elementAt = CodeInsightUtils.getTopmostElementAtOffset(
                sourcePosition.elementAt,
                file.getLineStartOffset(lineNumber) ?: sourcePosition.elementAt.startOffset
        ) ?: return null

        val start = elementAt.startOffset
        val end = elementAt.endOffset

        return CodeInsightUtils.
                findElementsOfClassInRange(file, start, end, JetFunctionLiteral::class.java, JetNamedFunction::class.java)
                .filter { JetPsiUtil.getParentCallIfPresent(it as JetExpression) != null }
                .filterIsInstance<JetFunction>()
                .filter {
                    val context = it.analyze(BodyResolveMode.PARTIAL)
                    InlineUtil.isInlinedArgument(it, context, false)
                }
    }
}