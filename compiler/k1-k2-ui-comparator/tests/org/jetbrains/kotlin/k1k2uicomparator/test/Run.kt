/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.k1k2uicomparator.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.k1k2uicomparator.components.UIComparatorFrame
import org.jetbrains.kotlin.k1k2uicomparator.components.UIComparatorStyle
import org.jetbrains.kotlin.k1k2uicomparator.support.spawn
import org.jetbrains.kotlin.test.runners.DiagnosticTestGenerated
import org.jetbrains.kotlin.test.runners.FirLightTreeOldFrontendDiagnosticsTestGenerated
import org.jetbrains.kotlin.test.runners.K1K2UIComparatorApi.compileSourceAndDecorateWithDiagnostics
import java.awt.EventQueue
import java.util.concurrent.atomic.AtomicBoolean

private val k1Runner = DiagnosticTestGenerated()
private val k2Runner = FirLightTreeOldFrontendDiagnosticsTestGenerated()

/**
 * The path supplied to the runners
 * for configuration purposes
 * (`forTestsMatching`).
 */
const val pathForConfigurationImitation = ""

val uiComparatorStyleForCompiler = UIComparatorStyle(
    title = "K1/K2 UI Comparator",
    initialSource = """
        fun main() {
            println("Done")
        }
    """.trimIndent(),
    leftViewerTitle = "K1",
    mainViewerTitle = "Clear Source",
    rightViewerTitle = "K2",
)

/**
 * Don't press the gutter icon if you are using
 * JPS, it won't work properly. Use the Gradle
 * task `runTest`.
 */
fun main() = EventQueue.invokeLater {
    val mainFrame = spawn { UIComparatorFrame(uiComparatorStyleForCompiler) }
    val coroutineScope = CoroutineScope(Dispatchers.Default)
    val isRecalculatingK1 = AtomicBoolean(false)
    val isRecalculatingK2 = AtomicBoolean(false)

    fun processK1(sourceInitially: String): String {
        return compileSourceAndDecorateWithDiagnostics(sourceInitially, k1Runner, pathForConfigurationImitation)
    }

    fun processK2(sourceInitially: String): String {
        return compileSourceAndDecorateWithDiagnostics(sourceInitially, k2Runner, pathForConfigurationImitation)
    }

    fun recalculate(
        sourceInitially: String,
        isRecalculating: AtomicBoolean,
        processSource: (String) -> String,
        setCode: (String) -> Unit,
    ) {
        if (sourceInitially != mainFrame.mainCode) {
            isRecalculating.compareAndSet(true, false)
            return recalculate(mainFrame.mainCode, isRecalculating, processSource, setCode)
        }

        if (!isRecalculating.compareAndSet(false, true)) {
            return
        }

        val result = try {
            processSource(sourceInitially)
        } catch (e: Exception) {
            e.stackTraceToString()
        } catch (e: junit.framework.AssertionFailedError) {
            e.stackTraceToString()
        } catch (e: org.opentest4j.AssertionFailedError) {
            e.stackTraceToString()
        }

        if (sourceInitially != mainFrame.mainCode) {
            isRecalculating.compareAndSet(true, false)
            return recalculate(mainFrame.mainCode, isRecalculating, processSource, setCode)
        }

        EventQueue.invokeLater {
            setCode(result)
        }

        isRecalculating.compareAndSet(true, false)
    }

    mainFrame.apply {
        coroutineScope.launch {
            recalculate(mainFrame.mainCode, isRecalculatingK1, ::processK1, mainFrame::setLeftCode)
            recalculate(mainFrame.mainCode, isRecalculatingK2, ::processK2, mainFrame::setRightCode)
        }

        addMainCodeChangeListener {
            coroutineScope.launch {
                recalculate(mainFrame.mainCode, isRecalculatingK1, ::processK1, mainFrame::setLeftCode)
                recalculate(mainFrame.mainCode, isRecalculatingK2, ::processK2, mainFrame::setRightCode)
            }
        }
    }
}
