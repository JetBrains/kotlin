/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.util.OldBackendStats

internal class BackendStatsCounter : IrVisitor<Unit, IrElement?>() {
    private val parentNodes: MutableSet<IrElement> = mutableSetOf()
    var nodesCount: Int = 0
        private set
    val backendStats: OldBackendStats
        get() = OldBackendStats(nodesCount, nodesCount - parentNodes.size)

    override fun visitElement(element: IrElement, data: IrElement?) {
        nodesCount++
        data?.let { parentNodes.add(data) }
        element.acceptChildren(this, element)
    }
}
