/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.symbols

import com.intellij.openapi.components.service
import com.intellij.openapi.util.io.FileUtil
import junit.framework.Assert
import org.jetbrains.kotlin.idea.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.fir.low.level.api.trackers.KotlinFirOutOfBlockModificationTrackerFactory
import org.jetbrains.kotlin.idea.frontend.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSessionProvider
import org.jetbrains.kotlin.idea.frontend.api.analyze
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSessionProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.EntityWasGarbageCollectedException
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import java.io.File

abstract class AbstractMemoryLeakInSymbolsTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun isFirPlugin() = true

    protected fun doTest(path: String) {
        val testDataFile = File(path)
        val ktFile = myFixture.configureByText(testDataFile.name, FileUtil.loadFile(testDataFile)) as KtFile
        val symbols = executeOnPooledThreadInReadAction {
            analyze(ktFile) {
                ktFile.collectDescendantsOfType<KtDeclaration>().map { it.getSymbol() }
            }
        }

        invalidateAllCaches(ktFile)
        System.gc()

        val leakedSymbols = executeOnPooledThreadInReadAction {
            symbols.map { it.hasNoFirElementLeak() }.filterIsInstance<LeakCheckResult.Leak>()
        }
        if (leakedSymbols.isNotEmpty()) {
            Assert.fail(
                """The following symbols leaked (${leakedSymbols.size}/${symbols.size})
                   ${leakedSymbols.joinToString(separator = "\n") { it.symbol }}
                   """.trimIndent()
            )
        }
    }

    @OptIn(InvalidWayOfUsingAnalysisSession::class)
    private fun invalidateAllCaches(ktFile: KtFile) {
        project.service<KotlinFirOutOfBlockModificationTrackerFactory>().incrementModificationsCount()
        (project.service<KtAnalysisSessionProvider>() as KtFirAnalysisSessionProvider).clearCaches()
        executeOnPooledThreadInReadAction { analyze(ktFile) {} }
    }

    private fun KtSymbol.hasNoFirElementLeak(): LeakCheckResult {
        require(this is KtFirSymbol<*>)
        return try {
            firRef.withFir { LeakCheckResult.Leak(this::class.simpleName!!) }
        } catch (_: EntityWasGarbageCollectedException) {
            LeakCheckResult.NoLeak
        }
    }

    private sealed class LeakCheckResult {
        object NoLeak : LeakCheckResult()
        data class Leak(val symbol: String) : LeakCheckResult()
    }
}

