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
import org.jetbrains.kotlin.backend.wasm.dwarf.lines.FileTable
import org.jetbrains.kotlin.wasm.ir.debug.DebugData
import org.jetbrains.kotlin.wasm.ir.debug.DebugInformation
import org.jetbrains.kotlin.wasm.ir.debug.DebugInformationGenerator
import org.jetbrains.kotlin.wasm.ir.debug.DebugSection
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocationMapping

class DwarfGenerator : DebugInformationGenerator {
    private val dwarf = Dwarf()

    private val sourceLocationMappings = mutableListOf<SourceLocationMappingWithPositionInFunction>()
    private val subprogramStack = mutableListOf<Subprogram>()

    override fun addSourceLocation(location: SourceLocationMapping) {
        sourceLocationMappings.add(SourceLocationMappingWithPositionInFunction(location))
    }

    override fun startFunction(location: SourceLocationMapping, name: String) {
        val sourceLocation = location.sourceLocation as? SourceLocation.Location ?: return
        val function = Subprogram(
            dwarf.strings.add(name),
            sourceLocation.fileId,
            location
        )

        sourceLocationMappings.add(SourceLocationMappingWithPositionInFunction(location, isFunctionStart = true))

        subprogramStack.push(function)
        dwarf.mainCompileUnit.children.add(function)
    }

    override fun endFunction(location: SourceLocationMapping) {
        if (location.sourceLocation !is SourceLocation.Location) return
        val function = subprogramStack.pop()
        sourceLocationMappings.add(SourceLocationMappingWithPositionInFunction(location, isFunctionEnd = true))
        function.endGeneratedLocation = location
    }

    override fun generateDebugInformation(): DebugInformation {
        var prev: SourceLocation.Location? = null

        for ((mapping, isFunctionStart, isFunctionEnd) in sourceLocationMappings) {
            val sourceLocation = mapping.sourceLocation.takeIf { it != prev || isFunctionEnd } as? SourceLocation.Location ?: continue
            val generatedLocation = mapping.generatedLocation
            val row = LineProgram.LineRow(
                sourceLocation.fileId,
                generatedLocation.column,
                sourceLocation.line,
                sourceLocation.column,
            )

            when {
                isFunctionStart -> dwarf.lines.startFunction(row)
                isFunctionEnd -> dwarf.lines.endFunction(row)
                else -> dwarf.lines.add(row)
            }

            prev = sourceLocation
        }

        return dwarf.generate().mapNotNull { section ->
            section
                .takeIf { it.offset != 0 }
                ?.let {
                    DebugSection(
                        it.name,
                        DebugData.RawBytes(it.toByteArray())
                    )
                }
        }
    }

    private val SourceLocation.Location.fileId: FileTable.FileId
        get() {
            val fileInfo = directoryAndFileName()
            return dwarf.lines.addFile(
                dwarf.strings.add(fileInfo.fileName),
                dwarf.strings.add(fileInfo.directoryPath),
            )
        }

    private fun SourceLocation.Location.directoryAndFileName(): FileInfo =
        when (file.indexOf('/')) {
            -1 -> FileInfo(".", file)
            0 -> FileInfo(".", file.substringAfterLast('/'))
            else -> FileInfo(file.substringBeforeLast('/'), file.substringAfterLast('/'))
        }

    private data class FileInfo(
        val directoryPath: String,
        val fileName: String,
    )

    private data class SourceLocationMappingWithPositionInFunction(
        val sourceLocationMapping: SourceLocationMapping,
        val isFunctionStart: Boolean = false,
        val isFunctionEnd: Boolean = false
    )
}