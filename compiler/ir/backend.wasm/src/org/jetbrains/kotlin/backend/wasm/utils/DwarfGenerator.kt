/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.utils

import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.wasm.dwarf.Dwarf
import org.jetbrains.kotlin.backend.wasm.dwarf.LineProgram
import org.jetbrains.kotlin.backend.wasm.dwarf.entries.Subprogram
import org.jetbrains.kotlin.backend.wasm.dwarf.lines.FileId
import org.jetbrains.kotlin.wasm.ir.debug.DebugData
import org.jetbrains.kotlin.wasm.ir.debug.DebugInformation
import org.jetbrains.kotlin.wasm.ir.debug.DebugInformationGenerator
import org.jetbrains.kotlin.wasm.ir.debug.DebugSection
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocationMapping

class DwarfGenerator : DebugInformationGenerator {
    private val dwarf = Dwarf()
    private val subprogramStack = mutableListOf<Subprogram>()
    private val sourceLocationMappings = mutableListOf<SourceLocationMappingWithPositionInFunction>()

    override fun addSourceLocation(location: SourceLocationMapping) {
        sourceLocationMappings.add(SourceLocationMappingWithPositionInFunction(location))
    }

    override fun startFunction(location: SourceLocationMapping, name: String) {
        val sourceLocation = location.sourceLocation as? SourceLocation.Location ?: return
        val function = Subprogram(dwarf.strings.add(name), sourceLocation.fileId, location)

        sourceLocationMappings.add(SourceLocationMappingWithPositionInFunction(location, PositionInFunction.START))

        subprogramStack.push(function)
        dwarf.mainCompileUnit.children.add(function)
    }

    override fun endFunction(location: SourceLocationMapping) {
        if (location.sourceLocation !is SourceLocation.Location) return
        val function = subprogramStack.pop()
        sourceLocationMappings.add(SourceLocationMappingWithPositionInFunction(location, PositionInFunction.END))
        function.endGeneratedLocation = location
    }

    override fun generateDebugInformation(): DebugInformation {
        var prev: SourceLocation.Location? = null

        for ((index, sourceLocationMapping) in sourceLocationMappings.withIndex()) {
            val (mapping, position) = sourceLocationMapping
            val sourceLocation = mapping.sourceLocation.takeIf { it != prev || position == PositionInFunction.END } as? SourceLocation.Location ?: continue
            val previousSourceLocationMapping = sourceLocationMappings.getOrNull(index - 1)?.sourceLocationMapping

            if (previousSourceLocationMapping != null && previousSourceLocationMapping.sourceLocation !is SourceLocation.Location) {
                dwarf.lines.addEmptyMapping(previousSourceLocationMapping.generatedLocationRelativeToCodeSection.column)
            }

            val generatedLocation = mapping.generatedLocationRelativeToCodeSection
            val row = LineProgram.LineRow(
                sourceLocation.fileId,
                generatedLocation.column,
                sourceLocation.line,
                sourceLocation.column,
            )

            when (position) {
                PositionInFunction.START -> dwarf.lines.startFunction(row)
                PositionInFunction.END -> dwarf.lines.endFunction(row)
                PositionInFunction.BODY -> dwarf.lines.add(row)
            }

            prev = sourceLocation
        }

        return dwarf.generate().mapNotNull { section ->
            section.takeIf { it.offset != 0 }
                ?.let { DebugSection(it.name, DebugData.RawBytes(it.toByteArray())) }
        }
    }

    private val SourceLocation.Location.fileId: FileId
        get() {
            val (fileName, directoryPath) = directoryAndFileName()
            return dwarf.lines.addFile(
                dwarf.lineStrings.add(fileName),
                dwarf.lineStrings.add(directoryPath),
            )
        }

    private fun SourceLocation.Location.directoryAndFileName(): Pair<String, String> =
        when (file.indexOf('/')) {
            -1 -> "." to file
            0 -> "." to file.substringAfterLast('/')
            else -> file.substringBeforeLast('/') to file.substringAfterLast('/')
        }

    private data class SourceLocationMappingWithPositionInFunction(
        val sourceLocationMapping: SourceLocationMapping,
        val positionInFunction: PositionInFunction = PositionInFunction.BODY,
    )

    private enum class PositionInFunction {
        START,
        BODY,
        END
    }
}