/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LowLevelFirApiFacadeForResolveOnAir
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirSourceModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AbstractLowLevelApiSingleFileTest
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFileAnnotationList
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.util.findElementByCommentPrefix

abstract class AbstractFirOnAirResolveTest : AbstractLowLevelApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        fun fixUpAnnotations(element: KtElement): KtElement = when (element) {
            is KtAnnotated -> element.annotationEntries.firstOrNull() ?: element
            is KtFileAnnotationList -> element.annotationEntries.first()
            else -> element
        }

        val place = (ktFile.findElementByCommentPrefix("/*PLACE*/") as KtElement).let(::fixUpAnnotations)
        val onAir = (ktFile.findElementByCommentPrefix("/*ONAIR*/") as KtElement).let(::fixUpAnnotations)

        check(place::class == onAir::class)

        resolveWithClearCaches(ktFile) { resolveState ->
            check(resolveState is LLFirSourceModuleResolveState)
            val firElement = LowLevelFirApiFacadeForResolveOnAir.onAirResolveElement(resolveState, place, onAir)
            val rendered = firElement.render(FirRenderer.RenderMode.WithResolvePhases)
            testServices.assertions.assertEqualsToTestDataFileSibling(rendered)
        }
    }

//    override val enableTestInDependedMode: Boolean get() = false
}