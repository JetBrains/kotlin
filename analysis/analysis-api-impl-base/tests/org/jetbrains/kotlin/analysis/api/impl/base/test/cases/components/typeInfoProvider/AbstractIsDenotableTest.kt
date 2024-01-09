/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.typeInfoProvider

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.project.structure.getKtFiles
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.types.Variance
import java.io.File

abstract class AbstractIsDenotableTest : AbstractAnalysisApiBasedTest() {
    val denotableName = Name.identifier("Denotable")
    val undenotableName = Name.identifier("Nondenotable")

    override fun doTestByMainModuleAndOptionalMainFile(mainFile: KtFile?, mainModule: TestModule, testServices: TestServices) {
        val ktFile = mainFile ?: testServices.ktModuleProvider.getKtFiles(mainModule).first()
        val actualText = buildString {
            ktFile.accept(object : KtTreeVisitorVoid() {
                override fun visitElement(element: PsiElement) {
                    if (element is LeafPsiElement) {
                        append(element.text)
                    }
                    super.visitElement(element)
                }


                override fun visitAnnotatedExpression(expression: KtAnnotatedExpression) {
                    val base = expression.baseExpression
                    if (base == null || expression.annotationEntries.none {
                            it.shortName == denotableName || it.shortName == undenotableName
                        }) {
                        super.visitAnnotatedExpression(expression)
                        return
                    }

                    analyseForTest(expression) {
                        val parent = expression.parentOfType<KtQualifiedExpression>()
                        // Try locating the containing PSI that is a receiver of a qualified expression because the smart cast information
                        // is only available at that level for FE1.0. For example, consider
                        // ```
                        // if (a is String) {
                        //   (@Denotable("...") a).length
                        // }
                        // ```
                        // smart cast is available for `(@Denotable("...") a)` and not for `a` or `@Denotable("...") a`.
                        val ktType = if (parent != null && deparenthesize(parent.receiverExpression) == deparenthesize(base)) {
                            parent.receiverExpression.getKtType()
                        } else {
                            expression.getKtType()
                        }
                        val actualHasDenotableType = ktType?.isDenotable ?: error("${base.text} does not have a type.")
                        when (actualHasDenotableType) {
                            true -> append("@Denotable")
                            false -> append("@Nondenotable")
                        }
                        append("(\"${ktType.render(position = Variance.INVARIANT)}\") ")
                        append(base.text)
                    }
                }
            })
        }
        testServices.assertions.assertEqualsToFile(testDataPath, actualText)
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useAdditionalSourceProviders(AbstractIsDenotableTest::TestHelperProvider)
        builder.defaultDirectives {
            +ConfigurationDirectives.WITH_STDLIB
        }
    }

    private class TestHelperProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
        override fun produceAdditionalFiles(globalDirectives: RegisteredDirectives, module: TestModule): List<TestFile> {
            return listOf(File("analysis/analysis-api/testData/helpers/isDenotable/helpers.kt").toTestFile())
        }
    }
}