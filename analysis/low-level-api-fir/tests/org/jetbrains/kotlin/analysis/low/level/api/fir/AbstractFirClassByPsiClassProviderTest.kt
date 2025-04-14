/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.providers.firClassByPsiClassProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractFirClassByPsiClassProviderTest : AbstractAnalysisApiBasedTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    override fun doTestByMainModuleAndOptionalMainFile(mainFile: KtFile?, mainModule: KtTestModule, testServices: TestServices) {
        val mainKtModule = mainModule.ktModule
        val psiClassUnderCaret = when (mainKtModule) {
            is KaSourceModule -> {
                testServices.expressionMarkerProvider.getBottommostElementsOfTypeAtCarets<PsiClass>(testServices).single().first
            }
            is KaLibraryModule -> {
                mainModule.psiFiles
                    .filterIsInstance<PsiJavaFile>()
                    .flatMap { it.classes.toList() }
                    .flatMap { it.withAllNestedClasses() }
                    .single { it.annotations.any { it.qualifiedName?.endsWith("Caret") == true } }
            }
            else -> {
                error("Unexpected module kind ${mainKtModule::class.simpleName}")
            }
        }

        val rendered = withResolutionFacade(mainKtModule) { resolutionFacade ->
            val firClassSymbol = resolutionFacade.useSiteFirSession.firClassByPsiClassProvider.getFirClass(psiClassUnderCaret)
            firClassSymbol.fir.render()
        }

        testServices.assertions.assertEqualsToTestOutputFile(rendered)
    }

    private fun PsiClass.withAllNestedClasses(): List<PsiClass> = buildList {
        fun collect(psiClass: PsiClass) {
            add(psiClass)
            psiClass.innerClasses.forEach(::collect)
        }
        collect(this@withAllNestedClasses)
    }
}
