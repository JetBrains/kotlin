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

        val pathResolver =
            SourceFilePathResolver.create(sourceMapsInfo.sourceRoots, sourceMapsInfo.sourceMapPrefix, sourceMapsInfo.outputDir)

        var prev: SourceLocation? = null
        var prevGeneratedLine = 0

        for (mapping in sourceLocationMappings) {
            val generatedLocation = mapping.generatedLocation
            val sourceLocation = mapping.sourceLocation.takeIf { it != prev || prevGeneratedLine != generatedLocation.line } ?: continue

            require(generatedLocation.line >= prevGeneratedLine) { "The order of the mapping is wrong" }

            if (prevGeneratedLine != generatedLocation.line) {
                repeat(generatedLocation.line - prevGeneratedLine) {
                    sourceMapBuilder.newLine()
                }
                prevGeneratedLine = generatedLocation.line
            }

            when (sourceLocation) {
                is SourceLocation.NoLocation -> sourceMapBuilder.addEmptyMapping(generatedLocation.column)
                is SourceLocation.Location -> {
                    sourceLocation.apply {
                        // TODO resulting path goes too deep since temporary directory we compiled first is deeper than final destination.
                        val relativePath = pathResolver.getPathRelativeToSourceRoots(File(file)).replace(Regex("^\\.\\./"), "")
                        sourceMapBuilder.addMapping(relativePath, null, { null }, line, column, null, generatedLocation.column)
                        prev = this
                    }
                }
            }

        }

        return sourceMapBuilder.build()
    }
}