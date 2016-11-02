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

package org.jetbrains.kotlin

import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.test.KotlinTestUtils
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoAfter
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.IdentifierInfo
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver

abstract class AbstractDataFlowValueRenderingTest: KotlinLightCodeInsightFixtureTestCase() {

    private fun IdentifierInfo.render(): String? = when (this) {
        is DataFlowValueFactory.ExpressionIdentifierInfo -> expression.text
        is IdentifierInfo.Receiver -> (value as? ImplicitReceiver)?.declarationDescriptor?.name?.let { "this@$it" }
        is IdentifierInfo.Variable -> variable.name.asString()
        is IdentifierInfo.PackageOrClass -> (descriptor as? PackageViewDescriptor)?.let { it.fqName.asString() }
        is IdentifierInfo.Qualified -> receiverInfo.render() + "." + selectorInfo.render()
        else -> null
    }

    private fun DataFlowValue.render() =
            // If it is not a stable identifier, there's no point in rendering it
            if (!isStable) null
            else identifierInfo.render()

    override fun getTestDataPath() : String {
        return PluginTestCaseBase.getTestDataPathBase() + "/dataFlowValueRendering/"
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return LightCodeInsightFixtureTestCase.JAVA_LATEST
    }

    fun doTest(fileName: String) {
        val fixture = myFixture
        fixture.configureByFile(fileName)

        val jetFile = fixture.file as KtFile
        val element = jetFile.findElementAt(fixture.caretOffset)!!
        val expression = element.getStrictParentOfType<KtExpression>()!!
        val info = expression.analyze().getDataFlowInfoAfter(expression)

        val allValues = (info.completeTypeInfo.keySet() + info.completeNullabilityInfo.keys).toSet()
        val actual = allValues.mapNotNull { it.render() }.sorted().joinToString("\n")

        KotlinTestUtils.assertEqualsToFile(File(FileUtil.getNameWithoutExtension(fileName) + ".txt"), actual)
    }
}
