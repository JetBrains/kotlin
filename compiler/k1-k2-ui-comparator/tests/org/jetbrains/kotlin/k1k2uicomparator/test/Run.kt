/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.k1k2uicomparator.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.kotlin.k1k2uicomparator.UIComparatorFrame
import org.jetbrains.kotlin.k1k2uicomparator.spawn
import org.jetbrains.kotlin.test.builders.testRunner
import org.jetbrains.kotlin.test.initIdeaConfiguration
import org.jetbrains.kotlin.test.runners.DiagnosticTestGenerated
import org.jetbrains.kotlin.test.runners.FirLightTreeOldFrontendDiagnosticsTestGenerated
import org.jetbrains.kotlin.test.services.KotlinTestInfo
import org.junit.jupiter.api.Test
import java.awt.EventQueue
import java.util.concurrent.atomic.AtomicBoolean

class TestRunner {
    @Test
    fun runMain() = main()
}

private val k1Runner = DiagnosticTestGenerated()
private val k2Runner = FirLightTreeOldFrontendDiagnosticsTestGenerated()

private val emptyKotlinTestInfo = KotlinTestInfo(
    className = "_undefined_",
    methodName = "_testUndefined_",
    tags = emptySet()
)

fun main() = EventQueue.invokeLater {
    val mainFrame = spawn(::UIComparatorFrame)
    val coroutineScope = CoroutineScope(Dispatchers.Default)
    val isRecalculatingK1 = AtomicBoolean(false)
    val isRecalculatingK2 = AtomicBoolean(false)

    fun processK1(sourceInitially: String): String {
        initIdeaConfiguration()
        k1Runner.initTestInfo(emptyKotlinTestInfo)
        return testRunner("", k1Runner.configuration).runTestSource(sourceInitially)
    }

    fun processK2(sourceInitially: String): String {
        initIdeaConfiguration()
        k2Runner.initTestInfo(emptyKotlinTestInfo)
        return testRunner("", k2Runner.configuration).runTestSource(sourceInitially)
    }

    fun recalculate(
        sourceInitially: String,
        isRecalculating: AtomicBoolean,
        processSource: (String) -> String,
        setCode: (String) -> Unit,
    ) {
        if (sourceInitially != mainFrame.mainCode) {
            isRecalculating.compareAndSet(true, false)
            return
        }

        if (!isRecalculating.compareAndSet(false, true)) {
            return
        }

        val result = processSource(sourceInitially)

        if (sourceInitially != mainFrame.mainCode) {
            isRecalculating.compareAndSet(true, false)
            return
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
