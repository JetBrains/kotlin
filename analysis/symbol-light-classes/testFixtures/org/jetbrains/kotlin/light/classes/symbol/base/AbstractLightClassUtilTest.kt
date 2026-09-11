/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.base

import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.LLSourceLikeTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.analysis.test.framework.targets.getSingleTestTargetSymbolOfType
import org.jetbrains.kotlin.light.classes.symbol.base.service.getLightElements
import org.jetbrains.kotlin.light.classes.symbol.base.service.getLightElementsFromDeclaration
import org.jetbrains.kotlin.light.classes.symbol.base.service.getLightElementsForJavaDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractLightClassUtilTest : AbstractAnalysisApiBasedTest() {
    override val configurator = LLSourceLikeTestConfigurator()

    override fun doTestByMainModuleAndOptionalMainFile(mainFile: KtFile?, mainModule: KtTestModule, testServices: TestServices) {
        val file = mainFile ?: mainModule.psiFiles.first()
        val lightElements = when (file) {
            is KtFile -> {
                val declaration = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaretOrNull<KtDeclaration>(file)
                analyzeForTest(file) {
                    // Declarations without PSI (e.g., intersection overrides) can be referenced by a fully qualified name
                    declaration?.getLightElements()
                        ?: getSingleTestTargetSymbolOfType<KaDeclarationSymbol>(testDataPath, file).getLightElementsFromDeclaration()
                }
            }
            is PsiJavaFile -> {
                val declaration = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<PsiMember>(file)
                analyze(mainModule.ktModule) {
                    declaration.getLightElementsForJavaDeclaration()
                }
            }
            else -> error("Unexpected file: ${file::class}")
        }

        val expectedLightElements = mainModule.testModule.directives[Directives.EXPECTED]

        testServices.assertions.assertEquals(expectedLightElements.size, lightElements.size) {
            "Found ${lightElements.map { "${it.javaClass.name}(${it.name})" }}"
        }

        lightElements.forEachIndexed { index, element ->
            testServices.assertions.assertEquals(expectedLightElements[index], "${element.javaClass.name}(${element.name})")
        }
    }

    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.defaultDirectives {
            +ConfigurationDirectives.WITH_STDLIB
        }
    }

    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    private object Directives : SimpleDirectivesContainer() {
        val EXPECTED by stringDirective(description = "Expected light classes")
    }
}
