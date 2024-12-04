/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.statistics.file

import java.util.*
import kotlin.math.max

internal fun printMap(p: Printer, name: String, mapping: Map<String, Int>) {
    if (mapping.isEmpty()) return

    if (mapping.size == 1) {
        p.println("$name: ${mapping.keys.single()}")
        return
    }

    p.withIndent("$name:") {
        val sortedEnumMap = mapping.toSortedMap()
        for ((k, v) in sortedEnumMap) {
            p.println("$k($v)")
        }
    }
}

internal class TextTable(vararg columnNames: String) {
    private val rows = ArrayList<List<String>>()
    private val columnsCount = columnNames.size
    private val maxLengths = IntArray(columnsCount) { columnNames[it].length }

    init {
        rows.add(columnNames.toList())
    }

    fun addRow(vararg row: String) {
        check(row.size == columnsCount) { "Row size ${row.size} differs from columns count $columnsCount" }
        rows.add(row.toList())

        for ((i, col) in row.withIndex()) {
            maxLengths[i] = max(maxLengths[i], col.length)
        }
    }

    fun printTo(p: Printer) {
        for (row in rows) {
            val rowStr = row.withIndex().joinToString("|") { (i, col) -> col.padEnd(maxLengths[i], ' ') }
            p.println(rowStr)
        }
    }
}
