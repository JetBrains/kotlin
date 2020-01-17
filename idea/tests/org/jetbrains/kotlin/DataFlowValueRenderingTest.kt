/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoAfter
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.IdentifierInfo
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractDataFlowValueRenderingTest: KotlinLightCodeInsightFixtureTestCase() {

    private fun IdentifierInfo.render(): String? = when (this) {
        is IdentifierInfo.Expression -> expression.text
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

    fun doTest(path: String) {
        val fixture = myFixture
        fixture.configureByFile(fileName())

        val jetFile = fixture.file as KtFile
        val element = jetFile.findElementAt(fixture.caretOffset)!!
        val expression = element.getStrictParentOfType<KtExpression>()!!
        val info = expression.analyze().getDataFlowInfoAfter(expression)

        val allValues = (info.completeTypeInfo.keySet() + info.completeNullabilityInfo.keySet()).toSet()
        val actual = allValues.mapNotNull { it.render() }.sorted().joinToString("\n")

        KotlinTestUtils.assertEqualsToFile(File(FileUtil.getNameWithoutExtension(path) + ".txt"), actual)
    }
}
