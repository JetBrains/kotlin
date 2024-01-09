/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeProvider

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.getKtFiles
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import java.io.File

abstract class AbstractHasCommonSubtypeTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainModuleAndOptionalMainFile(mainFile: KtFile?, mainModule: TestModule, testServices: TestServices) {
        val ktFile = mainFile ?: testServices.ktModuleProvider.getKtFiles(mainModule).first()
        val errors = mutableListOf<String>()
        val originalText = ktFile.text
        val actualTextBuilder = StringBuilder()
        analyseForTest(ktFile) {
            val visitor = object : KtTreeVisitorVoid() {
                override fun visitElement(element: PsiElement) {
                    if (element.firstChild == null) {
                        actualTextBuilder.append(element.text)
                    }
                    super.visitElement(element)
                }

                override fun visitCallExpression(expression: KtCallExpression) {
                    val haveCommonSubtype = when (expression.calleeExpression?.text) {
                        "typesHaveCommonSubtype" -> true
                        "typesHaveNoCommonSubtype" -> false
                        else -> {
                            super.visitCallExpression(expression)
                            return
                        }
                    }
                    val valueArguments = expression.valueArguments
                    require(valueArguments.size == 2) {
                        "Illegal call of ${expression.name} at ${expression.positionString}"
                    }

                    val a = valueArguments[0]
                    val aType = a.getArgumentExpression()?.getKtType()
                    if (aType == null) {
                        errors.add("'${a.text}' has no type at ${a.positionString}")
                        super.visitCallExpression(expression)
                        return
                    }
                    val b = valueArguments[1]
                    val bType = b.getArgumentExpression()?.getKtType()
                    if (bType == null) {
                        errors.add("'${b.text}' has no type at ${b.positionString}")
                        super.visitCallExpression(expression)
                        return
                    }
                    if (haveCommonSubtype != aType.hasCommonSubTypeWith(bType)) {
                        if (haveCommonSubtype) {
                            actualTextBuilder.append("typesHaveNoCommonSubtype")
                        } else {
                            actualTextBuilder.append("typesHaveCommonSubtype")
                        }
                        actualTextBuilder.append(expression.valueArgumentList!!.text)
                    } else {
                        super.visitCallExpression(expression)
                    }
                }
            }
            visitor.visitFile(ktFile)
        }
        if (errors.isNotEmpty()) {
            testServices.assertions.fail { errors.joinToString("\n") }
        }
        val actualText = actualTextBuilder.toString()
        if (actualText != originalText) {
            testServices.assertions.assertEqualsToFile(testDataPath, actualText)
        }
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useAdditionalSourceProviders(AbstractHasCommonSubtypeTest::TestHelperProvider)
        builder.defaultDirectives {
            +ConfigurationDirectives.WITH_STDLIB
        }
    }

    private class TestHelperProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
        override fun produceAdditionalFiles(globalDirectives: RegisteredDirectives, module: TestModule): List<TestFile> {
            return listOf(File("analysis/analysis-api/testData/helpers/hasCommonSubtype/helpers.kt").toTestFile())
        }
    }

    private val PsiElement.positionString: String
        get() {
            val illegalCallPos = StringUtil.offsetToLineColumn(containingFile.text, textRange.startOffset)
            return "${containingFile.virtualFile.path}:${illegalCallPos.line + 1}:${illegalCallPos.column + 1}"
        }
}