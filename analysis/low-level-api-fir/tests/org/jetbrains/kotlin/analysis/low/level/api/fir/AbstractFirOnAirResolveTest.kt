/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LowLevelFirApiFacadeForResolveOnAir
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AbstractLowLevelApiSingleFileTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFileAnnotationList
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.util.findElementByCommentPrefix

abstract class AbstractFirOnAirResolveTest : AbstractLowLevelApiSingleFileTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)

    override fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        fun fixUpAnnotations(element: KtElement): KtElement = when (element) {
            is KtAnnotated -> element.annotationEntries.firstOrNull() ?: element
            is KtFileAnnotationList -> element.annotationEntries.first()
            else -> element
        }

        val place = (ktFile.findElementByCommentPrefix("/*PLACE*/") as KtElement).let(::fixUpAnnotations)
        val onAir = (ktFile.findElementByCommentPrefix("/*ONAIR*/") as KtElement).let(::fixUpAnnotations)

        check(place::class == onAir::class)

        resolveWithClearCaches(ktFile) { firResolveSession ->
            check(firResolveSession.useSiteKtModule is KtSourceModule)
            val firElement = LowLevelFirApiFacadeForResolveOnAir.onAirResolveElement(firResolveSession, place, onAir)
            val rendered = FirRenderer.withResolvePhase().renderElementAsString(firElement)
            testServices.assertions.assertEqualsToTestDataFileSibling(rendered)
        }
    }
}