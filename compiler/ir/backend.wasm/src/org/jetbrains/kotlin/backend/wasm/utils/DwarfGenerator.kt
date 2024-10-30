/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.utils

import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.wasm.dwarf.Dwarf
import org.jetbrains.kotlin.backend.wasm.dwarf.LineProgram
import org.jetbrains.kotlin.wasm.ir.debug.DebugData
import org.jetbrains.kotlin.wasm.ir.debug.DebugInformation
import org.jetbrains.kotlin.wasm.ir.debug.DebugInformationGenerator
import org.jetbrains.kotlin.wasm.ir.debug.DebugSection
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocationMapping

class DwarfGenerator : DebugInformationGenerator {
    private val sourceLocationMappings = mutableListOf<SourceLocationMapping>()
    private val functions = mutableListOf<WasmFunction>()
    private val functionStack = mutableListOf<WasmFunction>()

    override fun addSourceLocation(location: SourceLocationMapping) {
        sourceLocationMappings.add(location)
    }

    override fun startFunction(location: SourceLocationMapping, name: String) {
        val function = WasmFunction(name, location)
        functionStack.push(function)
        functions.add(function)
    }

    override fun endFunction(location: SourceLocationMapping) {
        val function = functionStack.pop()
        function.endLocation = location
    }

    override fun generateDebugInformation(): DebugInformation {
        val dwarf = Dwarf()
        var prev: SourceLocation.Location? = null

        for (mapping in sourceLocationMappings) {
            val generatedLocation = mapping.generatedLocation
            val sourceLocation = mapping.sourceLocation.takeIf { it != prev } as? SourceLocation.Location ?: continue

            val fileName = sourceLocation.file.substringAfterLast('/')
            val dirName = sourceLocation.file.substringBeforeLast('/')

            dwarf.lines.add(
                LineProgram.LineRow(
                    dwarf.strings.add(fileName),
                    dwarf.strings.add(dirName),
                    generatedLocation.column,
                    sourceLocation.line,
                    sourceLocation.column
                )
            )

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

    private data class WasmFunction(val name: String, val startLocation: SourceLocationMapping) {
        lateinit var endLocation: SourceLocationMapping
    }
}