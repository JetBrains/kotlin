/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.codegen.inline.RangeMapping
import org.jetbrains.kotlin.codegen.inline.SMAPParser
import org.jetbrains.kotlin.codegen.inline.toRange
import org.jetbrains.kotlin.test.Assertions
import org.jetbrains.kotlin.utils.keysToMap
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import java.io.File

object CommonSMAPTestUtil {
    fun extractSMAPFromClasses(outputFiles: Iterable<OutputFile>): List<SMAPAndFile> {
        return outputFiles.map { outputFile ->
            var debugInfo: String? = null
            ClassReader(outputFile.asByteArray()).accept(object : ClassVisitor(Opcodes.API_VERSION) {
                override fun visitSource(source: String?, debug: String?) {
                    debugInfo = debug
                }
            }, 0)

            SMAPAndFile(debugInfo, outputFile.sourceFiles.single(), outputFile.relativePath)
        }
    }

    fun checkNoConflictMappings(compiledSmap: List<SMAPAndFile>?, assertions: Assertions) {
        if (compiledSmap == null) return

        compiledSmap.mapNotNull(SMAPAndFile::smap).forEach { smapString ->
            val smap = SMAPParser.parseOrNull(smapString) ?: throw AssertionError("bad SMAP: $smapString")
            val conflictingLines = smap.fileMappings.flatMap { fileMapping ->
                fileMapping.lineMappings.flatMap { lineMapping: RangeMapping ->
                    lineMapping.toRange.keysToMap { lineMapping }.entries
                }
            }.groupBy { it.key }.entries.filter { it.value.size != 1 }

            assertions.assertTrue(conflictingLines.isEmpty()) {
                conflictingLines.joinToString(separator = "\n") {
                    "Conflicting mapping for line ${it.key} in ${it.value.joinToString(transform = Any::toString)}"
                }
            }
        }
    }

    class SMAPAndFile(val smap: String?, val sourceFile: String, val outputFile: String) {
        constructor(smap: String?, sourceFile: File, outputFile: String) : this(smap, getPath(sourceFile), outputFile)

        companion object {
            fun getPath(file: File): String =
                getPath(file.canonicalPath)

            fun getPath(canonicalPath: String): String {
                //There are some problems with disk name on windows cause LightVirtualFile return it without disk name
                return FileUtil.toSystemIndependentName(canonicalPath).substringAfter(":")
            }
        }
    }
}
