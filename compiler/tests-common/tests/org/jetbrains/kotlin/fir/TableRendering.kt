/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import java.text.DecimalFormat

class RTableContext {
    val data: MutableList<Row> = mutableListOf()
    var cols = 0
    fun row(names: List<String>) = row(names.map { Cell(it) })

    sealed class Row {
        class Data(val cells: List<Cell>) : Row()
        class Separator() : Row()
    }

    @JvmName("rowCells")
    fun row(cells: List<Cell>) {
        cols = maxOf(cells.size, cols)
        data += Row.Data(cells)
    }

    fun row(vararg names: String) {
        row(listOf(*names))
    }

    fun separator() {
        data += Row.Separator()
    }

    inner class RTableRowContext() {
        val rowData = mutableListOf<Cell>()
        val LEFT = false
        val RIGHT = true
        fun cell(text: String?, align: Boolean = RIGHT) {
            rowData += Cell(text.toString(), align)
        }

        fun cells(texts: List<String>, align: Boolean = RIGHT) {
            rowData += texts.map { Cell(it, align) }
        }

        fun cells(vararg texts: String, align: Boolean = RIGHT) {
            cells(listOf(*texts), align)
        }
    }

    inline fun row(body: RTableRowContext.() -> Unit) {
        val ctx = RTableRowContext()
        ctx.body()
        row(ctx.rowData)
    }

    data class Cell(val text: String, val alignRight: Boolean = true) {
        fun padText(size: Int): String {
            return if (alignRight) {
                text.padStart(size)
            } else {
                text.padEnd(size)
            }
        }
    }

    fun printout(out: Appendable) {
        val colSize = IntArray(cols) { index ->
            data.filterIsInstance<Row.Data>().fold(0) { acc, row -> maxOf(acc, row.cells.getOrNull(index)?.text?.length ?: 0) }
        }


        fun appendHLine(prefix: String, sep: String, postfix: String) {
            out.append(prefix)
            for ((index, size) in colSize.withIndex()) {
                if (index != 0) {
                    out.append(sep)
                    out.append(HLINE)
                }
                out.append(HLINE.repeat(size))
            }
            out.append(postfix)
            out.appendLine()
        }

        appendHLine(CORNER_LU, TOP_T, CORNER_RU)
        for (row in data) {
            when (row) {
                is Row.Data -> {
                    out.append(VLINE)
                    for ((index, cell) in row.cells.withIndex()) {
                        out.append(cell.padText(colSize[index]))
                        out.append(VLINE)
                        out.append(" ")
                    }
                    out.appendLine()
                }
                is Row.Separator -> {
                    appendHLine(LEFT_T, CROSS, RIGHT_T)
                }
            }

        }
        appendHLine(CORNER_LD, BOT_T, CORNER_RD)
    }

    companion object {
        private const val CROSS = "┼"
        private const val VLINE = "│"
        private const val HLINE = "─"
        private const val CORNER_LU = "┌"
        private const val CORNER_RU = "┐"
        private const val CORNER_LD = "└"
        private const val CORNER_RD = "┘"
        private const val LEFT_T = "├"
        private const val RIGHT_T = "┤"
        private const val TOP_T = "┬"
        private const val BOT_T = "┴"
    }
}


enum class TableTimeUnit(val postfixText: String, val nsMultiplier: Double, val fractionDigits: Int) {
    NS("ns", 1.0, 0),
    MICS("mcs", 1e-3, 3),
    MS("ms", 1e-6, 6),
    S("s", 1e-9, 9);

    fun convert(value: Long, from: TableTimeUnit): Double {
        return value / from.nsMultiplier * this.nsMultiplier
    }
}

inline class TableTimeUnitConversion(val value: Double) {
    infix fun to(dest: TableTimeUnit): Double {
        return value * dest.nsMultiplier
    }
}

infix fun Long.from(from: TableTimeUnit) = TableTimeUnitConversion(this / from.nsMultiplier)


@Suppress("NOTHING_TO_INLINE")
inline fun RTableContext.RTableRowContext.timeCell(
    time: Long,
    outputUnit: TableTimeUnit = TableTimeUnit.MS,
    inputUnit: TableTimeUnit = TableTimeUnit.NS,
    fractionDigits: Int = outputUnit.fractionDigits
) {
    val df = DecimalFormat().apply {
        maximumFractionDigits = fractionDigits
        isGroupingUsed = true
    }
    cell("${df.format(outputUnit.convert(time, inputUnit))} ${outputUnit.postfixText}")
}

inline fun printTable(out: Appendable = System.out, body: RTableContext.() -> Unit) {
    RTableContext().apply(body).printout(out)
}