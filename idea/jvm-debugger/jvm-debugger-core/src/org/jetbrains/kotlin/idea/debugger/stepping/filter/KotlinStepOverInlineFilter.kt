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

import com.intellij.debugger.engine.DebugProcess.JAVA_STRATUM
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.openapi.project.Project
import com.intellij.util.Range
import com.sun.jdi.Location
import com.sun.jdi.StackFrame
import org.jetbrains.kotlin.codegen.inline.isFakeLocalVariableForInline
import org.jetbrains.kotlin.idea.debugger.safeLineNumber
import org.jetbrains.kotlin.idea.debugger.safeMethod
import org.jetbrains.kotlin.idea.debugger.safeVariables
import org.jetbrains.kotlin.idea.debugger.stepping.KotlinMethodFilter

@Suppress("EqualsOrHashCode")
data class StepOverCallerInfo(val declaringType: String, val methodName: String?, val methodSignature: String?) {
    companion object {
        fun from(location: Location): StepOverCallerInfo {
            val method = location.safeMethod()
            val declaringType = location.declaringType().name()
            val methodName = method?.name()
            val methodSignature = method?.signature()
            return StepOverCallerInfo(declaringType, methodName, methodSignature)
        }
    }
}

data class LocationToken(val lineNumber: Int, val inlineVariables: List<String>) {
    companion object {
        fun from(stackFrame: StackFrame): LocationToken {
            val location = stackFrame.location()
            val lineNumber = location.safeLineNumber(JAVA_STRATUM)
            val methodVariables = ArrayList<String>(0)

            for (variable in location.safeMethod()?.safeVariables() ?: emptyList()) {
                val name = variable.name()
                if (variable.isVisible(stackFrame) && isFakeLocalVariableForInline(name)) {
                    methodVariables += name
                }
            }

            return LocationToken(lineNumber, methodVariables)
        }
    }
}

class KotlinStepOverInlineFilter(
    val project: Project,
    private val tokensToSkip: Set<LocationToken>,
    private val callerInfo: StepOverCallerInfo
) : KotlinMethodFilter {
    override fun locationMatches(context: SuspendContextImpl, location: Location): Boolean {
        val stackFrame = context.frameProxy?.stackFrame ?: return true
        val token = LocationToken.from(stackFrame)
        val callerInfo = StepOverCallerInfo.from(location)

        if (callerInfo.methodName != null && callerInfo.methodSignature != null && this.callerInfo == callerInfo) {
            return token.lineNumber >= 0 && token !in tokensToSkip
        } else {
            return true
        }
    }

    override fun locationMatches(process: DebugProcessImpl, location: Location): Boolean {
        throw IllegalStateException("Should not be called from Kotlin hint")
    }

    override fun getCallingExpressionLines(): Range<Int>? = null
}