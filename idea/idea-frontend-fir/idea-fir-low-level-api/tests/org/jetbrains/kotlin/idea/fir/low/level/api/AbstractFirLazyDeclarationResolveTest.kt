/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.idea.fir.low.level.api.api.withFirDeclaration
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

/**
 * Test that we do not resolve declarations we do not need & do not build bodies for them
 */
abstract class AbstractFirLazyDeclarationResolveTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun isFirPlugin(): Boolean = true

    fun doTest(path: String) {
        val testDataFile = File(path)
        val ktFile = myFixture.configureByText(testDataFile.name, FileUtil.loadFile(testDataFile)) as KtFile
        val lazyDeclarations = ktFile.collectDescendantsOfType<KtDeclaration> { ktDeclaration -> !KtPsiUtil.isLocal(ktDeclaration) }

        val declarationToResolve = lazyDeclarations.firstOrNull { it.name?.toLowerCase() == "resolveme" }
            ?: error("declaration with name `resolveMe` was not found")
        resolveWithClearCaches(ktFile) { firModuleResolveState ->
            val rendered = declarationToResolve.withFirDeclaration(
                firModuleResolveState,
                FirResolvePhase.BODY_RESOLVE
            ) @Suppress("UNUSED_ANONYMOUS_PARAMETER") { firDeclaration ->
                val firFile = firModuleResolveState.getBuiltFirFileOrNull(ktFile)!!
                firFile.render(FirRenderer.RenderMode.WithResolvePhases)
            }
            val expectedFileName = testDataFile.name.replace(".kt", ".txt")
            KotlinTestUtils.assertEqualsToFile(testDataFile.parentFile.resolve(expectedFileName), rendered)
        }
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}