/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.util.AnalysisStats
import org.jetbrains.kotlin.util.Time


internal class FirStatsCounter : FirVisitor<Unit, FirElement?>() {
    private val parentNodes: MutableSet<FirElement> = mutableSetOf()
    var starImportCount: Int = 0
        private set
    var nodesCount: Int = 0
        private set
    val analysisStats: AnalysisStats
        get() = AnalysisStats(Time.ZERO, nodesCount, nodesCount - parentNodes.size, starImportCount)

    fun reset() {
        starImportCount = 0
        nodesCount = 0
        parentNodes.clear()
    }

    override fun visitElement(element: FirElement, data: FirElement?) {
        addStatsAndAcceptChildren(element, data)
    }

    override fun visitImport(import: FirImport, data: FirElement?) {
        if (import.isAllUnder) {
            starImportCount++
        }
        addStatsAndAcceptChildren(import, data)
    }

    private fun addStatsAndAcceptChildren(element: FirElement, parent: FirElement?) {
        nodesCount++
        parent?.let { parentNodes.add(parent) }
        element.acceptChildren(this, element)
    }
}