/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.debugger.stepping.filter

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.openapi.project.Project
import com.intellij.util.Range
import com.sun.jdi.LocalVariable
import com.sun.jdi.Location
import org.jetbrains.kotlin.idea.debugger.ktLocationInfo
import org.jetbrains.kotlin.idea.debugger.stepping.KotlinMethodFilter
import org.jetbrains.kotlin.idea.debugger.stepping.getInlineRangeLocalVariables

class StepOverFilterData(
    val lineNumber: Int,
    val stepOverLines: Set<Int>,
    val inlineRangeVariables: List<LocalVariable>,
    val isDexDebug: Boolean,
    val skipAfterCodeIndex: Long = -1
)

class KotlinStepOverInlineFilter(val project: Project, val data: StepOverFilterData) : KotlinMethodFilter {
    private fun Location.ktLineNumber() = ktLocationInfo(this, data.isDexDebug, project).first

    override fun locationMatches(context: SuspendContextImpl, location: Location): Boolean {
        val frameProxy = context.frameProxy ?: return true

        if (data.skipAfterCodeIndex != -1L && location.codeIndex() > data.skipAfterCodeIndex) {
            return false
        }

        val currentLine = location.ktLineNumber()
        if (!(data.stepOverLines.contains(currentLine))) {
            return currentLine != data.lineNumber
        }

        val visibleInlineVariables = getInlineRangeLocalVariables(frameProxy)

        // Our ranges check missed exit from inline function. This is when breakpoint was in last statement of inline functions.
        // This can be observed by inline local range-variables. Absence of any means step out was done.
        return data.inlineRangeVariables.any { !visibleInlineVariables.contains(it) }
    }

    override fun locationMatches(process: DebugProcessImpl, location: Location): Boolean {
        throw IllegalStateException() // Should not be called from Kotlin hint
    }

    override fun getCallingExpressionLines(): Range<Int>? = null
}