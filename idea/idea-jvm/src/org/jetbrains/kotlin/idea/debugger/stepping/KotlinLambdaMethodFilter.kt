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
import com.intellij.debugger.engine.BreakpointStepMethodFilter
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.util.Range
import com.sun.jdi.Location
import org.jetbrains.kotlin.codegen.coroutines.DO_RESUME_METHOD_NAME
import org.jetbrains.kotlin.idea.refactoring.isMultiLine
import org.jetbrains.kotlin.idea.debugger.isInsideInlineArgument
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.util.OperatorNameConventions

class KotlinLambdaMethodFilter(
        private val lambda: KtFunction,
        private val myCallingExpressionLines: Range<Int>,
        private val isInline: Boolean,
        private val isSuspend: Boolean
): BreakpointStepMethodFilter {
    private val myFirstStatementPosition: SourcePosition?
    private val myLastStatementLine: Int

    init {
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
            myLastStatementLine = if (lastStatementPosition != null) lastStatementPosition.line else -1
        }
        else {
            myFirstStatementPosition = SourcePosition.createFromElement(lambda)
            myLastStatementLine = myFirstStatementPosition!!.line
        }
    }

    override fun getBreakpointPosition() = myFirstStatementPosition
    override fun getLastStatementLine() = myLastStatementLine

    override fun locationMatches(process: DebugProcessImpl, location: Location): Boolean {
        val method = location.method()

        if (isInline) {
            return isInsideInlineArgument(lambda, location, process)
        }

        return isLambdaName(method.name())
    }

    override fun getCallingExpressionLines() = if (isInline) Range(0, 999) else myCallingExpressionLines

    private fun isLambdaName(name: String?): Boolean {
        if (isSuspend) {
            return name == DO_RESUME_METHOD_NAME
        }
        
        return name == OperatorNameConventions.INVOKE.asString()
    }

}