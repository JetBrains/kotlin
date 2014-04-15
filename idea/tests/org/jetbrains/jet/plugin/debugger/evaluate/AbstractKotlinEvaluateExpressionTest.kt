/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.debugger.evaluate

import com.intellij.debugger.DebuggerInvocationUtil
import com.intellij.debugger.engine.evaluation.expression.EvaluatorBuilderImpl
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.engine.evaluation.CodeFragmentKind
import com.intellij.debugger.engine.ContextUtil
import com.intellij.debugger.engine.SuspendContextImpl
import org.jetbrains.jet.plugin.debugger.*
import org.junit.Assert
import org.jetbrains.jet.plugin.JetFileType
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import org.jetbrains.jet.InTextDirectivesUtils
import org.jetbrains.eval4j.jdi.asValue
import org.jetbrains.eval4j.Value
import org.jetbrains.eval4j.ObjectValue
import com.sun.jdi.ObjectReference

public abstract class AbstractKotlinEvaluateExpressionTest : KotlinDebuggerTestCase() {
    fun doTest(path: String) {
        val fileContent = FileUtil.loadFile(File(path))
        val expressions = InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileContent, "// EXPRESSION: ")
        val expectedResults = InTextDirectivesUtils.findLinesWithPrefixesRemoved(fileContent, "// RESULT: ")

        assert(expressions.size == expectedResults.size, "Sizes of test directives are different")

        createDebugProcess(path)

        onBreakpoint {
            val exceptions = linkedMapOf<String, Throwable>()
            for ((i, expression) in expressions.withIndices()) {
                try {
                    evaluate(expression, expectedResults[i])
                }
                catch (e: Throwable) {
                    exceptions.put(expression, e)
                }
            }

            if (!exceptions.empty) {
                for (exc in exceptions.values()) {
                    exc.printStackTrace()
                }
                throw AssertionError("Test failed:\n" + exceptions.map { "expression: ${it.key}, exception: ${it.value.getMessage()}" }.makeString("\n"))
            }
        }
        finish()
    }

    private fun onBreakpoint(doOnBreakpoint: SuspendContextImpl.() -> Unit) {
        super.onBreakpoint {
            super.printContext(it)
            it.doOnBreakpoint()
        }
    }

    private fun finish() {
        onBreakpoint {
            resume(this)
        }
    }

    private fun SuspendContextImpl.evaluate(expression: String, expectedResult: String) {
        try {
            val sourcePosition = ContextUtil.getSourcePosition(this)
            val contextElement = ContextUtil.getContextElement(sourcePosition)!!
            Assert.assertTrue("KotlinCodeFragmentFactory should be accepted for context element otherwise default evaluator will be called. ContextElement = ${contextElement.getText()}",
                              KotlinCodeFragmentFactory().isContextAccepted(contextElement))

            val evaluator = DebuggerInvocationUtil.commitAndRunReadAction(getProject()) {
                EvaluatorBuilderImpl.build(TextWithImportsImpl(CodeFragmentKind.EXPRESSION, expression, KotlinEditorTextProvider.getImports(contextElement), JetFileType.INSTANCE),
                                           contextElement,
                                           sourcePosition)
            }
            if (evaluator == null) throw AssertionError("Cannot create an Evaluator for Evaluate Expression")

            val value = evaluator.evaluate(createEvaluationContext(this))
            val actualResult = value.asValue().asString()
            Assert.assertTrue("Evaluate expression returns wrong result for $expression:\nexpected = $expectedResult\nactual   = $actualResult\n", expectedResult == actualResult)
        }
        finally {
            resume(this)
        }
    }

    private fun Value.asString(): String {
        if (this is ObjectValue && this.value is ObjectReference) {
            return this.toString().replaceFirst("id=[0-9]*", "id=ID")
        }
        return this.toString()
    }
}