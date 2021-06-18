/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.frontend.api.symbols

import com.intellij.openapi.components.service
import com.intellij.openapi.util.io.FileUtil
import com.sun.management.HotSpotDiagnosticMXBean
import junit.framework.Assert
import org.jetbrains.kotlin.idea.caches.project.LibraryModificationTracker
import org.jetbrains.kotlin.idea.fir.executeOnPooledThreadInReadAction
import org.jetbrains.kotlin.idea.fir.low.level.api.api.KotlinOutOfBlockModificationTrackerFactory
import org.jetbrains.kotlin.idea.fir.low.level.api.trackers.KotlinFirOutOfBlockModificationTrackerFactory
import org.jetbrains.kotlin.idea.frontend.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSessionProvider
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSessionProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import java.io.File
import java.lang.management.ManagementFactory
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

abstract class AbstractMemoryLeakInSymbolsTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun isFirPlugin() = true

    protected fun doTest(path: String) {
        val testDataFile = File(path)
        val ktFile = myFixture.configureByText(testDataFile.name, FileUtil.loadFile(testDataFile)) as KtFile
        val symbols = executeOnPooledThreadInReadAction {
            analyse(ktFile) {
                ktFile.collectDescendantsOfType<KtDeclaration>().map { it.getSymbol() }
            }
        }

        invalidateAllCaches(ktFile)
        System.gc()

        val leakedSymbols = executeOnPooledThreadInReadAction {
            symbols.map { it.hasNoFirElementLeak() }.filterIsInstance<LeakCheckResult.Leak>()
        }
        if (leakedSymbols.isNotEmpty()) {
            val memoryDumpPath = dumpHeap()
            Assert.fail(
                """Some symbols leaked (${leakedSymbols.size}/${symbols.size})
                   Memory dump is $memoryDumpPath
                   Symbols:
                   ${leakedSymbols.joinToString(separator = "\n") { it.symbol }}
                   """.trimIndent()
            )
        }
    }

    @OptIn(InvalidWayOfUsingAnalysisSession::class)
    private fun invalidateAllCaches(ktFile: KtFile) {
        (project.service<KotlinOutOfBlockModificationTrackerFactory>() as KotlinFirOutOfBlockModificationTrackerFactory).incrementModificationsCount()
        project.service<LibraryModificationTracker>().incModificationCount()
        (project.service<KtAnalysisSessionProvider>() as KtFirAnalysisSessionProvider).clearCaches()
        executeOnPooledThreadInReadAction { analyse(ktFile) {} }
    }

    private fun KtSymbol.hasNoFirElementLeak(): LeakCheckResult {
        require(this is KtFirSymbol<*>)
        return if (firRef.isCollected()) LeakCheckResult.NoLeak
        else LeakCheckResult.Leak(this::class.simpleName!!)
    }

    private sealed class LeakCheckResult {
        object NoLeak : LeakCheckResult()
        data class Leak(val symbol: String) : LeakCheckResult()
    }

    private fun dumpHeap(): Path {
        val fileNameSuffix = SimpleDateFormat("yyMMddHHmmss").format(Date())
        val path = Paths.get(basePath).resolve("memoryLeakDump_${fileNameSuffix}.hprof")
        val server = ManagementFactory.getPlatformMBeanServer()
        ManagementFactory.newPlatformMXBeanProxy(
            server,
            "com.sun.management:type=HotSpotDiagnostic",
            HotSpotDiagnosticMXBean::class.java
        ).dumpHeap(path.toString(), true)
        return path.toAbsolutePath()
    }
}
