/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.frontend.api.components

import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.fir.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.fir.invalidateCaches
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.frontend.api.components.KtTypeRendererOptions
import org.jetbrains.kotlin.idea.frontend.api.components.buildClassType
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.types.KtTypeNullability
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtFile
import org.junit.Test

internal class TypeCreatorTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun isFirPlugin() = true

    override fun tearDown() {
        project.invalidateCaches(file as? KtFile)
        super.tearDown()
    }

    private fun doTest(
        expected: String,
        buildType: KtAnalysisSession.() -> KtType,
    ) {
        val fakeKtFile = myFixture.configureByText("file.kt", "val a = 10") as KtFile
        val renderedType = executeOnPooledThreadInReadAction {
            analyse(fakeKtFile) {
                val ktType = buildType()
                ktType.render(RENDERING_OPTIONS)
            }
        }
        assertEquals(expected, renderedType)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    @Test
    fun testClassTypeByClassId() {
        doTest("List<Int?>") {
            buildClassType(StandardClassIds.List) {
                argument(buildClassType(StandardClassIds.Int) { nullability = KtTypeNullability.NULLABLE })
            }
        }
    }

    @Test
    fun testClassTypeBySymbolClassId() {
        doTest("MutableList<Int>") {
            val listSymbol = StandardClassIds.MutableList.getCorrespondingToplevelClassOrObjectSymbol()!!
            buildClassType(listSymbol) {
                argument(buildClassType(StandardClassIds.Int))
            }
        }
    }

    @Test
    fun testClassTypeByUnresolvedClassId() {
        doTest("ERROR_TYPE <Symbol not found for /NonExistingListClass>") {
            buildClassType(ClassId.fromString("NonExistingListClass")) {
                argument(buildClassType(StandardClassIds.Int) { nullability = KtTypeNullability.NULLABLE })
            }
        }
    }


    companion object {
        private val RENDERING_OPTIONS = KtTypeRendererOptions.SHORT_NAMES
    }
}