/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.utils

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.SourceMapsInfo
import org.jetbrains.kotlin.js.sourceMap.SourceFilePathResolver
import org.jetbrains.kotlin.js.sourceMap.SourceMap3Builder
import org.jetbrains.kotlin.wasm.ir.debug.DebugData
import org.jetbrains.kotlin.wasm.ir.debug.DebugInformation
import org.jetbrains.kotlin.wasm.ir.debug.DebugInformationGenerator
import org.jetbrains.kotlin.wasm.ir.debug.DebugSection
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocation
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocationMapping
import java.io.File

class SourceMapGenerator(
    baseFileName: String,
    private val configuration: CompilerConfiguration
) : DebugInformationGenerator {
    // TODO: eliminate duplication for the [org.jetbrains.kotlin.backend.wasm.writeCompilationResult] logic
    private val sourceMapFileName = "$baseFileName.map"
    private val sourceLocationMappings = mutableListOf<SourceLocationMapping>()

    override fun addSourceLocation(location: SourceLocationMapping) {
        sourceLocationMappings.add(location)
    }

    override fun generateDebugInformation(): DebugInformation {
        return listOf(DebugSection("sourceMappingURL", DebugData.StringData(sourceMapFileName)))
    }

    fun generate(): String? {
        val sourceMapsInfo = SourceMapsInfo.from(configuration) ?: return null

        val sourceMapBuilder =
            SourceMap3Builder(null, { error("This should not be called for Kotlin/Wasm") }, sourceMapsInfo.sourceMapPrefix)
                .apply { addIgnoredSource(SourceLocation.IgnoredLocation.file) }

        val pathResolver = SourceFilePathResolver.create(
            sourceMapsInfo.sourceRoots,
            sourceMapsInfo.sourceMapPrefix,
            sourceMapsInfo.outputDir,
        )

        var prev: SourceLocation? = null
        var prevGeneratedLine = 0
        var offsetExpectedNextLocation = -1

        for (mapping in sourceLocationMappings) {
            val generatedLocation = mapping.generatedLocation
            val sourceLocation = mapping.sourceLocation.takeIf { it != prev || prevGeneratedLine != generatedLocation.line }

            if (sourceLocation == null) {
                offsetExpectedNextLocation = -1
                continue
            }

            require(generatedLocation.line >= prevGeneratedLine) { "The order of the mapping is wrong" }

            if (prevGeneratedLine != generatedLocation.line) {
                repeat(generatedLocation.line - prevGeneratedLine) {
                    sourceMapBuilder.newLine()
                }
                prevGeneratedLine = generatedLocation.line
            }

            when (sourceLocation) {
                SourceLocation.NoLocation -> continue
                SourceLocation.NextLocation -> {
                    if (offsetExpectedNextLocation == -1) offsetExpectedNextLocation = generatedLocation.column
                }
                is SourceLocation.WithFileAndLineNumberInformation -> {
                    // TODO resulting path goes too deep since temporary directory we compiled first is deeper than final destination.
                    val relativePath = if (sourceLocation is SourceLocation.DefinedLocation) {
                        pathResolver.getPathRelativeToSourceRoots(File(sourceLocation.file)).replace(Regex("^\\.\\./"), "")
                    } else sourceLocation.file

                    if (offsetExpectedNextLocation != -1) {
                        sourceMapBuilder.addMapping(
                            relativePath,
                            sourceLocation.line,
                            sourceLocation.column,
                            offsetExpectedNextLocation
                        )
                        offsetExpectedNextLocation = -1
                    }

                    sourceMapBuilder.addMapping(
                        relativePath,
                        sourceLocation.line,
                        sourceLocation.column,
                        generatedLocation.column
                    )

                    prev = sourceLocation
                }
            }

        }

        return sourceMapBuilder.build()
    }
}