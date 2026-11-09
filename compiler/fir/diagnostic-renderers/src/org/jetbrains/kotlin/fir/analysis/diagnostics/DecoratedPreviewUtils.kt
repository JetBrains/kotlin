/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.getTightOffsetsFor
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.session.sourcesToPathsMapper
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirBackingFieldSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.render
import org.jetbrains.kotlin.toLinesMapping
import org.jetbrains.kotlin.utils.addToStdlib.partitionNotNull
import org.jetbrains.kotlin.utils.addToStdlib.popLast
import org.jetbrains.kotlin.utils.addToStdlib.runIf

data class LogicalLocation(
    val packageName: String,
    val moduleName: Name,
)

fun FirBasedSymbol<*>.toLogicalLocation(): LogicalLocation {
    if (this is FirTypeParameterSymbol) return containingDeclarationSymbol.toLogicalLocation()
    val packageName = if (packageFqName().isRoot) "root package" else packageFqName().render()
    val moduleName = moduleData.name
    return LogicalLocation(packageName, moduleName)
}

data class PhysicalLocation(
    val filePath: String,
    val line: Int,
    val column: Int,
    val endLine: Int,
)

fun FirBasedSymbol<*>.toPhysicalLocation(): PhysicalLocation? {
    val source = source
    val filePath = source?.let(moduleData.session.sourcesToPathsMapper::getRelativeSourceFilePath) ?: return null
    val linesMapping = source.toLinesMapping()
    // We don't render surrounding fillers in `FirDiagnosticRenderers.SYMBOL` et al.
    val [startOffset, endOffset] = getTightOffsetsFor(source)
    val [line, column] = linesMapping.getLineAndColumnByOffset(startOffset)
        .let { [line, column] -> (line + 1) to (column + 1) }
    val endLine = linesMapping.getLineAndColumnByOffset(endOffset).first + 1
    return PhysicalLocation(filePath, line, column, endLine)
}

internal data class LogicalLocationNode(
    val location: LogicalLocation,
    val completeSymbols: List<FileNode>,
    val incompleteSymbols: List<SymbolNode>,
)

internal data class FileNode(
    val filePath: String,
    val symbols: List<SymbolNode>,
)

data class SymbolNode(
    val line: Int?,
    val column: Int?,
    val endLine: Int?,
    val symbol: FirBasedSymbol<*>,
    var children: MutableCollection<SymbolNode>,
)

private fun FirBasedSymbol<*>.toSymbolNode(children: MutableCollection<SymbolNode> = mutableListOf()): SymbolNode {
    val physicalLocation = toPhysicalLocation()
    return SymbolNode(physicalLocation?.line, physicalLocation?.column, physicalLocation?.endLine, this, children)
}

private fun FirBasedSymbol<*>.collectContainers(): List<FirBasedSymbol<*>> =
    generateSequence(getContainer()) { it.getContainer() }.toList().reversed()

private fun FirBasedSymbol<*>.getContainer(): FirBasedSymbol<*>? =
    (this as? FirPropertyAccessorSymbol)?.propertySymbol
        ?: (this as? FirBackingFieldSymbol)?.propertySymbol
        ?: getContainingClassSymbol()

fun Iterable<FirBasedSymbol<*>>.groupIntoSymbolNodes(): List<SymbolNode> {
    val containerToNode = mutableMapOf<FirBasedSymbol<*>, SymbolNode>()
    val symbolChains = map { symbol ->
        val chain = symbol.collectContainers() + symbol
        chain.map { containerToNode.getOrPut(it) { it.toSymbolNode(children = mutableSetOf()) } }
    }
    for (chain in symbolChains) {
        for (index in 1 until chain.size) {
            chain[index - 1].children.add(chain[index])
        }
    }
    for (chain in symbolChains) {
        for (node in chain) {
            node.children = node.children.sortedByDescending { it.line ?: 0 }.toMutableList()
        }
    }
    return symbolChains.mapTo(mutableSetOf()) { it.first() }.sortedBy { it.line }
}

private fun Iterable<Pair<String, FirBasedSymbol<*>>>.groupIntoFileNodes(): List<FileNode> =
    groupBy({ it.first }, { it.second }).entries.map {
        FileNode(it.key, it.value.groupIntoSymbolNodes())
    }

internal fun Iterable<FirBasedSymbol<*>>.groupIntoLogicalLocations(): List<LogicalLocationNode> =
    groupBy { it.toLogicalLocation() }.map { [location, symbols] ->
        val [completeSymbols, incompleteSymbols] = symbols.partitionNotNull { it.toPhysicalLocation()?.filePath?.to(it) }
        LogicalLocationNode(location, completeSymbols.groupIntoFileNodes(), incompleteSymbols.groupIntoSymbolNodes())
    }

data class RenderedLineWithNumber(
    val number: Int?,
    val indent: Int,
    val renderedLine: String,
)

inline fun FirBasedSymbol<*>.renderToLinesWithNumber(render: (FirBasedSymbol<*>) -> String): List<RenderedLineWithNumber> =
    render(this).lines().mapIndexed { index, line ->
        RenderedLineWithNumber(number = if (index == 0) toPhysicalLocation()?.line else null, indent = 0, renderedLine = line)
    }

inline fun SymbolNode.renderToLinesWithNumber(render: (FirBasedSymbol<*>) -> String): List<RenderedLineWithNumber> {
    val orderStack = mutableListOf(0 to this)
    var previousIndent = -1
    val parentStack = mutableListOf<SymbolNode>()

    return buildList {
        while (orderStack.isNotEmpty()) {
            val [indent, element] = orderStack.popLast()

            if (indent < previousIndent) {
                parentStack.popLast().takeIf { it.symbol !is FirPropertySymbol }?.let { parent ->
                    this += RenderedLineWithNumber(number = parent.endLine, indent = indent, renderedLine = "}")
                }
            }

            val ownLines = element.symbol.renderToLinesWithNumber(render).map { it.copy(indent = indent) }

            if (element.children.isNotEmpty()) {
                if (element.symbol !is FirPropertySymbol) {
                    this += ownLines.dropLast(1)
                    this += ownLines.last().copy(renderedLine = ownLines.last().renderedLine + " {")
                } else {
                    this += ownLines
                }
                orderStack += element.children.map { indent + 1 to it }
                parentStack += element
            } else {
                this += ownLines
            }

            previousIndent = indent
        }

        while (parentStack.isNotEmpty()) {
            parentStack.popLast().takeIf { it.symbol !is FirPropertySymbol }?.let { parent ->
                this += RenderedLineWithNumber(number = parent.endLine, indent = parentStack.size, renderedLine = "}")
            }
        }
    }
}

internal inline fun Iterable<FirBasedSymbol<*>>.renderWithLocationInGroups(render: (FirBasedSymbol<*>) -> String): String =
    groupIntoLogicalLocations()
        .flatMap { location ->
            val blocksForComplete = location.completeSymbols.map {
                renderSymbolsWithLocation(location.location, it.filePath, it.symbols, render)
            }
            val blockForIncomplete = runIf(location.incompleteSymbols.isNotEmpty()) {
                renderSymbolsWithLocation(location.location, filePath = null, location.incompleteSymbols, render)
            }
            blocksForComplete + listOfNotNull(blockForIncomplete)
        }
        .joinToString("\n")

inline fun FirBasedSymbol<*>.renderWithLocation(render: (FirBasedSymbol<*>) -> String): String {
    val logicalLocation = toLogicalLocation()
    val node = listOf(this).groupIntoSymbolNodes()
    return when (val physicalLocation = toPhysicalLocation()) {
        null -> renderSymbolsWithLocation(logicalLocation, filePath = null, node, render)
        else -> renderSymbolsWithLocation(logicalLocation, physicalLocation.filePath, node, render)
    }
}

inline fun renderSymbolsWithLocation(
    logicalLocation: LogicalLocation,
    filePath: String?,
    symbols: Collection<SymbolNode>,
    render: (FirBasedSymbol<*>) -> String,
): String = renderLocationDecoration(
    header = DecorationHeader.Candidates(logicalLocation, filePath),
    renderedLines = symbols.flatMap { symbol -> symbol.renderToLinesWithNumber(render) }
        .interleaveWith { previous, next ->
            val areSiblings = previous.number != null && previous.number + 1 == next.number
            val arePartsOfSingleRealSource = previous.number != null && previous.number == next.number
            when {
                areSiblings || arePartsOfSingleRealSource -> null
                else -> RenderedLineWithNumber(number = null, indent = maxOf(previous.indent, next.indent), renderedLine = "...")
            }
        },
    padding = DecorationPadding.PAD_TO_AT_LEAST_3_LINES,
)

fun renderLocationDecoration(
    header: DecorationHeader,
    renderedLines: List<RenderedLineWithNumber>,
    padding: DecorationPadding = DecorationPadding.PAD_TO_AT_LEAST_3_LINES,
): String {
    val longestNumberString = renderedLines.mapNotNull { it.number?.toString() }.maxByOrNull { it.length } ?: ""
    val indentString = " ".repeat(longestNumberString.length)
    var previousNumber: Int? = null

    val paddedLines = renderedLines.map {
        val numberString = it.number?.takeIf { it != previousNumber }?.toString()?.padStart(indentString.length, ' ') ?: indentString
        previousNumber = it.number
        "$numberString | " + "    ".repeat(it.indent) + it.renderedLine
    }
    val paddedRenderedLines = paddedLines.joinToString("\n")
    val padding = DecorationPadding(
        padTop = padding != DecorationPadding.NONE && paddedLines.size <= 1,
        padBottom = padding != DecorationPadding.NONE && paddedLines.size <= 2,
    )

    return renderLocationDecoration(header, indentString, paddedRenderedLines, padding)
}

inline fun <T> List<T>.interleaveWith(produceInterleave: (T, T) -> T?): List<T> {
    if (size <= 1) return this

    return buildList {
        add(this@interleaveWith.first())

        for (it in 1 until this@interleaveWith.size) {
            produceInterleave(this@interleaveWith[it - 1], this@interleaveWith[it])?.let(::add)
            add(this@interleaveWith[it])
        }
    }
}

sealed class DecorationHeader {
    data class Candidates(val logicalLocation: LogicalLocation, val filePath: String?) : DecorationHeader()
    data object None : DecorationHeader()
}

class DecorationPadding(val padTop: Boolean, val padBottom: Boolean) {
    companion object {
        val PAD_TO_AT_LEAST_3_LINES: DecorationPadding = DecorationPadding(padTop = false, padBottom = true)
        val NONE: DecorationPadding = DecorationPadding(padTop = false, padBottom = false)
    }
}

private fun renderLocationDecoration(
    header: DecorationHeader,
    indentString: String,
    content: String,
    padding: DecorationPadding = DecorationPadding.PAD_TO_AT_LEAST_3_LINES,
): String = listOfNotNull(
    when (header) {
        is Candidates -> header.logicalLocation.let { [packageName, moduleName] -> "In package '$packageName' of module '$moduleName':" }
        is None -> null
    },
    when (header) {
        is Candidates -> header.filePath?.let { "$indentString--> $it" }
        is None -> null
    },
    "$indentString |".takeIf { padding.padTop },
    content,
    "$indentString |".takeIf { padding.padBottom },
).joinToString("\n")
