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
import com.intellij.debugger.engine.ContextUtil
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.events.DebuggerCommandImpl
import com.intellij.debugger.impl.JvmSteppingCommandProvider
import com.intellij.util.concurrency.Semaphore
import com.intellij.xdebugger.impl.XSourcePositionImpl
import com.sun.jdi.ReferenceType
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.core.refactoring.getLineCount
import org.jetbrains.kotlin.idea.core.refactoring.getLineStartOffset
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
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

        val semaphore = Semaphore()
        semaphore.down()

        var sourcePosition : SourcePosition? = null
        var allClasses: List<ReferenceType>? = null
        val worker = object : DebuggerCommandImpl() {
            override fun action() {
                try {
                    sourcePosition = runReadAction { ContextUtil.getSourcePosition(suspendContext) }
                    if (sourcePosition != null) {
                        allClasses = suspendContext.debugProcess.positionManager.getAllClasses(sourcePosition!!)
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

        val computedSourcePosition = sourcePosition ?: return null
        val computedReferenceType = allClasses?.firstOrNull() ?: return null

        val file = computedSourcePosition.file as? JetFile ?: return null

        val inlinedArguments = getInlinedArgumentsIfAny(computedSourcePosition) ?: return null

        val locations = computedReferenceType.allLineLocations()
        val countOfLinesInFile = file.getLineCount()

        val nextLine = computedSourcePosition.line + 2 /* +1 - because of locations are counted from 1 and +1 - because we want next line */

        for (lineNumber in nextLine..countOfLinesInFile) {
            val location = locations.firstOrNull { it.lineNumber() == lineNumber }
            if (location != null) {
                val lineStartOffset = file.getLineStartOffset(lineNumber - 1) ?: continue
                if (inlinedArguments.any { it.textRange.contains(lineStartOffset) }) continue

                val elementAt = file.findElementAt(lineStartOffset)
                val xPosition = XSourcePositionImpl.createByElement(elementAt) ?: return null

                return suspendContext.debugProcess.createRunToCursorCommand(suspendContext, xPosition, ignoreBreakpoints)
            }
        }

        return null
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