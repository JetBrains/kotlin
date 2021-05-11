/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.idea.fir.low.level.api.api.LowLevelFirApiFacadeForResolveOnAir
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFileAnnotationList
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.findElementByCommentPrefix
import java.io.File

abstract class AbstractFirOnAirResolveTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun isFirPlugin(): Boolean = true

    fun doTest(path: String) {
        val testDataFile = File(path)
        val ktFile = myFixture.configureByText(testDataFile.name, FileUtil.loadFile(testDataFile)) as KtFile

        fun fixUpAnnotations(element: KtElement): KtElement = when (element) {
            is KtAnnotated -> element.annotationEntries.firstOrNull() ?: element
            is KtFileAnnotationList -> element.annotationEntries.first()
            else -> element
        }

        val place = (ktFile.findElementByCommentPrefix("/*PLACE*/") as KtElement).let(::fixUpAnnotations)
        val onAir = (ktFile.findElementByCommentPrefix("/*ONAIR*/") as KtElement).let(::fixUpAnnotations)

        check(place::class == onAir::class)

        resolveWithClearCaches(ktFile) { firModuleResolveState ->
            check(firModuleResolveState is FirModuleResolveStateImpl)
            val firElement = LowLevelFirApiFacadeForResolveOnAir.onAirResolveElement(firModuleResolveState, place, onAir)
            val rendered = firElement.render(FirRenderer.RenderMode.WithResolvePhases)
            val expectedFileName = testDataFile.name.replace(".kt", ".txt")
            KotlinTestUtils.assertEqualsToFile(testDataFile.parentFile.resolve(expectedFileName), rendered)
        }
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}