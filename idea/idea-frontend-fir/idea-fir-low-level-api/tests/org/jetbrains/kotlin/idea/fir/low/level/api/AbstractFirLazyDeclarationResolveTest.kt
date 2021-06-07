/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.fir.low.level.api.api.withFirDeclaration
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.ResolveType
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.declarationCanBeLazilyResolved
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

/**
 * Test that we do not resolve declarations we do not need & do not build bodies for them
 */
abstract class AbstractFirLazyDeclarationResolveTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun isFirPlugin(): Boolean = true

    private fun FirFile.findResolveMe(): FirDeclaration {
        val visitor = object : FirVisitorVoid() {
            var result: FirDeclaration? = null
            override fun visitElement(element: FirElement) {
                if (result != null) return
                val declaration = element.realPsi as? KtDeclaration
                if (element is FirDeclaration && declaration != null && declaration.name == "resolveMe") {
                    result = element
                    return
                }
                element.acceptChildren(this)
            }

        }
        accept(visitor)
        return visitor.result ?: error("declaration with name `resolveMe` was not found")
    }


    fun doTest(path: String) {
        val testDataFile = File(path)
        val ktFile = myFixture.configureByText(testDataFile.name, FileUtil.loadFile(testDataFile)) as KtFile

        val resultBuilder = StringBuilder()
        resolveWithClearCaches(ktFile) { firModuleResolveState ->
            check(firModuleResolveState is FirModuleResolveStateImpl)
            val declarationToResolve = firModuleResolveState
                .getOrBuildFirFile(ktFile)
                .findResolveMe()
            for (currentPhase in FirResolvePhase.values()) {
                if (currentPhase.pluginPhase || currentPhase == FirResolvePhase.SEALED_CLASS_INHERITORS) continue
                declarationToResolve.withFirDeclaration(firModuleResolveState, currentPhase) {
                    val firFile = firModuleResolveState.getOrBuildFirFile(ktFile)
                    resultBuilder.append("\n${currentPhase.name}:\n")
                    resultBuilder.append(firFile.render(FirRenderer.RenderMode.WithDeclarationAttributes))
                }
            }
        }

        for (resolveType in ResolveType.values()) {
            resolveWithClearCaches(ktFile) { firModuleResolveState ->
                check(firModuleResolveState is FirModuleResolveStateImpl)
                val declarationToResolve = firModuleResolveState
                    .getOrBuildFirFile(ktFile)
                    .findResolveMe()

                when (resolveType) {
                    ResolveType.CallableReturnType,
                    ResolveType.CallableBodyResolve,
                    ResolveType.CallableContracts -> if (declarationToResolve !is FirCallableDeclaration<*>) return@resolveWithClearCaches
                    ResolveType.ClassSuperTypes -> if (declarationToResolve !is FirClassLikeDeclaration<*>) return@resolveWithClearCaches
                    else -> {
                    }
                }

                declarationToResolve.withFirDeclaration(resolveType, firModuleResolveState) {
                    val firFile = firModuleResolveState.getOrBuildFirFile(ktFile)
                    resultBuilder.append("\n${resolveType.name}:\n")
                    resultBuilder.append(firFile.render(FirRenderer.RenderMode.WithDeclarationAttributes))
                }
            }
        }

        resolveWithClearCaches(ktFile) { firModuleResolveState ->
            check(firModuleResolveState is FirModuleResolveStateImpl)
            val firFile = firModuleResolveState.getOrBuildFirFile(ktFile)
            firFile.withFirDeclaration(firModuleResolveState, FirResolvePhase.BODY_RESOLVE) {
                resultBuilder.append("\nFILE RAW TO BODY:\n")
                resultBuilder.append(firFile.render(FirRenderer.RenderMode.WithDeclarationAttributes))
            }
        }

        val expectedFileName = testDataFile.name.replace(".kt", ".txt")
        KotlinTestUtils.assertEqualsToFile(testDataFile.parentFile.resolve(expectedFileName), resultBuilder.toString())
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}