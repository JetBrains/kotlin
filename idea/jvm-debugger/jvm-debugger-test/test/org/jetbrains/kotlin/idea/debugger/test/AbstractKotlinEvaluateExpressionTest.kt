/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test

import com.intellij.debugger.engine.ContextUtil
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.evaluation.CodeFragmentKind
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl
import com.intellij.debugger.engine.evaluation.expression.EvaluatorBuilderImpl
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.impl.DebuggerContextImpl.createDebuggerContext
import com.intellij.debugger.ui.impl.watch.NodeDescriptorImpl
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.treeStructure.Tree
import com.intellij.xdebugger.impl.ui.tree.ValueMarkup
import com.sun.jdi.ObjectReference
import org.jetbrains.eval4j.ObjectValue
import org.jetbrains.eval4j.Value
import org.jetbrains.eval4j.jdi.asValue
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinCodeFragmentFactory
import org.jetbrains.kotlin.idea.debugger.test.preference.DebuggerPreferences
import org.jetbrains.kotlin.idea.debugger.test.util.FramePrinter
import org.jetbrains.kotlin.idea.debugger.test.util.FramePrinterDelegate
import org.jetbrains.kotlin.idea.debugger.test.util.KotlinOutputChecker
import org.jetbrains.kotlin.idea.debugger.test.util.SteppingInstruction
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.InTextDirectivesUtils.findLinesWithPrefixesRemoved
import org.jetbrains.kotlin.test.InTextDirectivesUtils.findStringWithPrefixes
import org.jetbrains.kotlin.test.KotlinBaseTest
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.swing.tree.TreeNode

private data class CodeFragment(val text: String, val result: String, val kind: CodeFragmentKind)

private data class DebugLabel(val name: String, val localName: String)

private class EvaluationTestData(
    val instructions: List<SteppingInstruction>,
    val fragments: List<CodeFragment>,
    val debugLabels: List<DebugLabel>
)

abstract class AbstractKotlinEvaluateExpressionTest : KotlinDescriptorTestCaseWithStepping(), FramePrinterDelegate {
    private companion object {
        private val ID_PART_REGEX = "id=[0-9]*".toRegex()
    }

    override val debuggerContext: DebuggerContextImpl
        get() = super.debuggerContext

    private var isMultipleBreakpointsTest = false

    private var framePrinter: FramePrinter? = null

    private val exceptions = ConcurrentHashMap<String, Throwable>()

    override fun runBare() {
        // DO NOTHING
    }

    fun doSingleBreakpointTest(path: String) {
        isMultipleBreakpointsTest = false
        doTest(path)
    }

    fun doMultipleBreakpointsTest(path: String) {
        isMultipleBreakpointsTest = true
        doTest(path)
    }

    override fun doMultiFileTest(files: TestFiles, preferences: DebuggerPreferences) {
        val wholeFile = files.wholeFile

        val instructions = SteppingInstruction.parse(wholeFile)
        val expressions = loadExpressions(wholeFile)
        val blocks = loadCodeBlocks(files.originalFile)
        val debugLabels = loadDebugLabels(wholeFile)

        val data = EvaluationTestData(instructions, expressions + blocks, debugLabels)

        framePrinter = FramePrinter(myDebuggerSession, this, preferences, testRootDisposable)

        if (isMultipleBreakpointsTest) {
            performMultipleBreakpointTest(data)
        } else {
            performSingleBreakpointTest(data)
        }
    }

    override fun tearDown() {
        framePrinter?.close()
        framePrinter = null
        exceptions.clear()

        super.tearDown()
    }

    private fun performSingleBreakpointTest(data: EvaluationTestData) {
        process(data.instructions)

        doOnBreakpoint {
            createDebugLabels(data.debugLabels)

            val exceptions = linkedMapOf<String, Throwable>()

            for ((expression, expected, kind) in data.fragments) {
                mayThrow(expression) {
                    evaluate(this, expression, kind, expected)
                }
            }

            val completion = { resume(this) }
            framePrinter?.printFrame(completion) ?: completion()
        }

        finish()
    }

    private fun performMultipleBreakpointTest(data: EvaluationTestData) {
        for ((expression, expected) in data.fragments) {
            doOnBreakpoint {
                mayThrow(expression) {
                    try {
                        evaluate(this, expression, CodeFragmentKind.EXPRESSION, expected)
                    } finally {
                        val completion = { resume(this) }
                        framePrinter?.printFrame(completion) ?: completion()
                    }
                }
            }
        }
        finish()
    }

    override fun evaluate(suspendContext: SuspendContextImpl, textWithImports: TextWithImportsImpl) {
        evaluate(suspendContext, textWithImports, null)
    }

    private fun evaluate(suspendContext: SuspendContextImpl, text: String, codeFragmentKind: CodeFragmentKind, expectedResult: String?) {
        val textWithImports = TextWithImportsImpl(codeFragmentKind, text, "", KotlinFileType.INSTANCE)
        return evaluate(suspendContext, textWithImports, expectedResult)
    }

    private fun evaluate(suspendContext: SuspendContextImpl, item: TextWithImportsImpl, expectedResult: String?) {
        val evaluationContext = this.evaluationContext
        val sourcePosition = ContextUtil.getSourcePosition(suspendContext)

        // Default test debuggerContext doesn't provide a valid stackFrame so we have to create one more for evaluation purposes.
        val frameProxy = suspendContext.frameProxy
        val threadProxy = frameProxy?.threadProxy()
        val debuggerContext = createDebuggerContext(myDebuggerSession, suspendContext, threadProxy, frameProxy)
        debuggerContext.initCaches()

        val contextElement = ContextUtil.getContextElement(debuggerContext)!!

        assert(KotlinCodeFragmentFactory().isContextAccepted(contextElement)) {
            val text = runReadAction { contextElement.text }
            "KotlinCodeFragmentFactory should be accepted for context element otherwise default evaluator will be called. " +
                    "ContextElement = $text"
        }

        contextElement.putCopyableUserData(KotlinCodeFragmentFactory.DEBUG_CONTEXT_FOR_TESTS, debuggerContext)

        suspendContext.runActionInSuspendCommand {
            try {
                val evaluator = runReadAction {
                    EvaluatorBuilderImpl.build(
                        item,
                        contextElement,
                        sourcePosition,
                        this@AbstractKotlinEvaluateExpressionTest.project
                    )
                }
                    ?: throw AssertionError("Cannot create an Evaluator for Evaluate Expression")

                val value = evaluator.evaluate(evaluationContext)
                val actualResult = value.asValue().asString()
                if (expectedResult != null) {
                    assertEquals(
                        "Evaluate expression returns wrong result for ${item.text}:\n" +
                                "expected = $expectedResult\n" +
                                "actual   = $actualResult\n",
                        expectedResult, actualResult
                    )
                }
            } catch (e: EvaluateException) {
                val expectedMessage = e.message?.replaceFirst(
                    ID_PART_REGEX,
                    "id=ID"
                )
                assertEquals(
                    "Evaluate expression throws wrong exception for ${item.text}:\n" +
                            "expected = $expectedResult\n" +
                            "actual   = $expectedMessage\n",
                    expectedResult,
                    expectedMessage
                )
            }
        }
    }

    override fun logDescriptor(descriptor: NodeDescriptorImpl, text: String) {
        super.logDescriptor(descriptor, text)
    }

    override fun expandAll(tree: Tree, runnable: () -> Unit, filter: (TreeNode) -> Boolean, suspendContext: SuspendContextImpl) {
        super.expandAll(tree, runnable, HashSet(), filter, suspendContext)
    }

    private fun mayThrow(expression: String, f: () -> Unit) {
        try {
            f()
        } catch (e: Throwable) {
            exceptions[expression] = e
        }
    }

    override fun throwExceptionsIfAny() {
        if (exceptions.isNotEmpty()) {
            val isIgnored = InTextDirectivesUtils.isIgnoredTarget(
                if (useIrBackend()) TargetBackend.JVM_IR else TargetBackend.JVM,
                getExpectedOutputFile()
            )

            if (!isIgnored) {
                for (exc in exceptions.values) {
                    exc.printStackTrace()
                }
                val expressionsText = exceptions.entries.joinToString("\n") { (k, v) -> "expression: $k, exception: ${v.message}" }
                throw AssertionError("Test failed:\n$expressionsText")
            } else {
                (checker as KotlinOutputChecker).threwException = true
            }
        }
    }

    private fun Value.asString(): String {
        if (this is ObjectValue && this.value is ObjectReference) {
            return this.toString().replaceFirst(ID_PART_REGEX, "id=ID")
        }
        return this.toString()
    }

    private fun loadExpressions(testFile: KotlinBaseTest.TestFile): List<CodeFragment> {
        val directives = findLinesWithPrefixesRemoved(testFile.content, "// EXPRESSION: ")
        val expected = findLinesWithPrefixesRemoved(testFile.content, "// RESULT: ")
        assert(directives.size == expected.size) { "Sizes of test directives are different" }
        return directives.zip(expected).map { (text, result) -> CodeFragment(text, result, CodeFragmentKind.EXPRESSION) }
    }

    private fun loadCodeBlocks(wholeFile: File): List<CodeFragment> {
        val regexp = (Regex.escape(wholeFile.name) + ".fragment\\d*").toRegex()
        val fragmentFiles = wholeFile.parentFile.listFiles { _, name -> regexp.matches(name) } ?: emptyArray()

        val codeFragments = mutableListOf<CodeFragment>()

        for (fragmentFile in fragmentFiles) {
            val contents = FileUtil.loadFile(fragmentFile, true)
            val value = findStringWithPrefixes(contents, "// RESULT: ") ?: error("'RESULT' directive is missing in $fragmentFile")
            codeFragments += CodeFragment(contents, value, CodeFragmentKind.CODE_BLOCK)
        }

        return codeFragments
    }

    private fun loadDebugLabels(testFile: KotlinBaseTest.TestFile): List<DebugLabel> {
        return findLinesWithPrefixesRemoved(testFile.content, "// DEBUG_LABEL: ")
            .map { text ->
                val labelParts = text.split("=")
                assert(labelParts.size == 2) { "Wrong format for DEBUG_LABEL directive: // DEBUG_LABEL: {localVariableName} = {labelText}" }

                val localName = labelParts[0].trim()
                val name = labelParts[1].trim()
                DebugLabel(name, localName)
            }
    }

    private fun createDebugLabels(labels: List<DebugLabel>) {
        if (labels.isEmpty()) {
            return
        }

        val markupMap = NodeDescriptorImpl.getMarkupMap(debugProcess) ?: return

        for ((name, localName) in labels) {
            val localVariable = evaluationContext.frameProxy!!.visibleVariableByName(localName)
            assert(localVariable != null) { "Cannot find localVariable for label: name = $localName" }

            val localVariableValue = evaluationContext.frameProxy!!.getValue(localVariable) as? ObjectReference
            assert(localVariableValue != null) { "Local variable $localName should be an ObjectReference" }

            markupMap[localVariableValue] = ValueMarkup(name, null, name)
        }
    }
}
