/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.resolve

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFir
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirOfType
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getResolveState
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractInnerDeclarationsResolvePhaseTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun isFirPlugin(): Boolean = true
    fun doTest(path: String) {
        val testDataFile = File(path)
        val ktFile = myFixture.configureByText(testDataFile.name, FileUtil.loadFile(testDataFile)) as KtFile

        val resolveState = ktFile.getResolveState()
        val firFile = ktFile.getOrBuildFirOfType<FirFile>(resolveState)

        val actual = firFile.render(FirRenderer.RenderMode.WithResolvePhases)
        val expectedFileName = testDataFile.name.replace(".kt", ".fir.txt")
        KotlinTestUtils.assertEqualsToFile(testDataFile.parentFile.resolve(expectedFileName), actual)
    }
}
