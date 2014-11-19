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

package org.jetbrains.jet.resolve

import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.JetTestCaseBuilder
import org.jetbrains.jet.plugin.JetWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import java.util.HashSet
import org.jetbrains.jet.JetTestUtils
import java.io.File
import org.jetbrains.jet.lang.psi.JetBlockExpression
import org.junit.Assert
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.renderer.DescriptorRenderer
import org.jetbrains.jet.lang.descriptors.VariableDescriptor
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.psi.psiUtil.getReceiverExpression
import org.jetbrains.jet.plugin.caches.resolve.getResolutionFacade
import org.jetbrains.jet.lang.psi.psiUtil.parents

public abstract class AbstractPartialBodyResolveTest : JetLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath() = JetTestCaseBuilder.getHomeDirectory()
    override fun getProjectDescriptor() = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    public fun doTest(testPath: String) {
        myFixture.configureByFile(testPath)

        val file = myFixture.getFile() as JetFile
        val offset = myFixture.getEditor().getCaretModel().getOffset()
        val element = file.findElementAt(offset)
        val refExpression = element.getParentByType(javaClass<JetSimpleNameExpression>()) ?: error("No JetSimpleNameExpression at caret")

        val resolutionFacade = file.getResolutionFacade()

        // optimized resolve
        val (target1, type1, processedStatements1) = doResolve(refExpression, resolutionFacade.analyzeWithPartialBodyResolve(refExpression))

        // full body resolve
        val (target2, type2, processedStatements2) = doResolve(refExpression, resolutionFacade.analyze(refExpression))

        val set = HashSet(processedStatements2)
        assert (set.containsAll(processedStatements1))
        set.removeAll(processedStatements1)

        val builder = StringBuilder()
        builder.append("Resolve target: ${target2.presentation(type2)}\n")
        builder.append("Skipped statements:\n")
        set.sortBy { it.getTextOffset() }.forEach {
            if (!it.parents(withItself = false).any { it in set }) { // do not dump skipped statements which are inside other skipped statement
                builder append it.presentation() append "\n"
            }
        }

        JetTestUtils.assertEqualsToFile(File(testPath.substringBeforeLast('.') + ".dump"), builder.toString())

        //TODO: discuss that descriptors are different
        Assert.assertEquals(target2.presentation(type2), target1.presentation(type1))
    }

    private data class ResolveData(
            val target: DeclarationDescriptor?,
            val type: JetType?,
            val processedStatements: Collection<JetExpression>
    )

    private fun doResolve(refExpression: JetSimpleNameExpression, bindingContext: BindingContext): ResolveData {
        val target = bindingContext[BindingContext.REFERENCE_TARGET, refExpression]

        val processedStatements = bindingContext.getSliceContents(BindingContext.PROCESSED)
                .filter { it.value }
                .map { it.key }
                .filter { it.getParent() is JetBlockExpression }

        val receiver = refExpression.getReceiverExpression()
        val expressionWithType = if (receiver != null) {
            refExpression.getParent() as? JetExpression ?: refExpression
        }
        else {
            refExpression
        }
        val type = bindingContext[BindingContext.EXPRESSION_TYPE, expressionWithType]

        return ResolveData(target, type, processedStatements)
    }

    private fun DeclarationDescriptor?.presentation(type: JetType?): String {
        if (this == null) return "null"

        val s = DescriptorRenderer.COMPACT.render(this)

        val renderType = this is VariableDescriptor && type != this.getReturnType()
        if (!renderType) return s
        return s + " smart-casted to " + if (type != null) DescriptorRenderer.COMPACT.renderType(type) else "unknown type"
    }

    private fun JetExpression.presentation(): String {
        val text = getText()
        val builder = StringBuilder()
        var dropSpace = false
        for (c in text) {
            when (c) {
                ' ', '\n', '\r' -> {
                    if (!dropSpace) builder.append(' ')
                    dropSpace = true
                }

                else -> {
                    builder.append(c)
                    dropSpace = false
                }
            }
        }
        return builder.toString()
    }
}