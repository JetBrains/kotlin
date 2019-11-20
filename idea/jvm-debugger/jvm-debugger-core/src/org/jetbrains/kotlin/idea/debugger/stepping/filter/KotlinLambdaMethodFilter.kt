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

package org.jetbrains.kotlin.idea.debugger.stepping.filter

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.BreakpointStepMethodFilter
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.util.Range
import com.sun.jdi.Location
import org.jetbrains.kotlin.codegen.coroutines.isResumeImplMethodNameFromAnyLanguageSettings
import org.jetbrains.kotlin.idea.core.util.isMultiLine
import org.jetbrains.kotlin.idea.debugger.isInsideInlineArgument
import org.jetbrains.kotlin.idea.debugger.safeMethod
import org.jetbrains.kotlin.idea.debugger.stepping.smartStepInto.KotlinLambdaSmartStepTarget
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.util.OperatorNameConventions

class KotlinLambdaMethodFilter(target: KotlinLambdaSmartStepTarget) : BreakpointStepMethodFilter {
    private val lambdaPtr = target.getLambda().createSmartPointer()
    private val myCallingExpressionLines: Range<Int>? = target.callingExpressionLines
    private val isInline = target.isInline
    private val isSuspend = target.isSuspend

    private val myFirstStatementPosition: SourcePosition?
    private val myLastStatementLine: Int

    init {
        val lambda = target.getLambda()
        val body = lambda.bodyExpression
        if (body != null && lambda.isMultiLine()) {
            var firstStatementPosition: SourcePosition? = null
            var lastStatementPosition: SourcePosition? = null
            val statements = (body as? KtBlockExpression)?.statements ?: listOf(body)
            if (statements.isNotEmpty()) {
                firstStatementPosition = SourcePosition.createFromElement(statements.first())
                if (firstStatementPosition != null) {
                    val lastStatement = statements.last()
                    lastStatementPosition = SourcePosition.createFromOffset(firstStatementPosition.file, lastStatement.textRange.endOffset)
                }
            }
            myFirstStatementPosition = firstStatementPosition
            myLastStatementLine = lastStatementPosition?.line ?: -1
        } else {
            myFirstStatementPosition = SourcePosition.createFromElement(lambda)
            myLastStatementLine = myFirstStatementPosition!!.line
        }
    }

    override fun getBreakpointPosition() = myFirstStatementPosition
    override fun getLastStatementLine() = myLastStatementLine

    override fun locationMatches(process: DebugProcessImpl, location: Location): Boolean {
        val lambda = runReadAction { lambdaPtr.element } ?: return true

        if (isInline) {
            return isInsideInlineArgument(lambda, location, process)
        }

        val method = location.safeMethod() ?: return true
        return isLambdaName(method.name())
    }

    override fun getCallingExpressionLines() = if (isInline) Range(0, Int.MAX_VALUE) else myCallingExpressionLines

    private fun isLambdaName(name: String?): Boolean {
        if (isSuspend && name != null) {
            return isResumeImplMethodNameFromAnyLanguageSettings(name)
        }

        return name == OperatorNameConventions.INVOKE.asString()
    }

}
