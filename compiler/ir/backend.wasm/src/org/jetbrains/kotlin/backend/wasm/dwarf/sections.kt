/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf

import org.jetbrains.kotlin.wasm.ir.convertors.ByteWriter
import java.io.ByteArrayOutputStream

sealed class DebuggingSection(val name: String) {
    private val os = ByteArrayOutputStream()

    val offset get() = writer.written
    val writer = ByteWriter.OutputStream(os)

    fun toByteArray(): ByteArray = os.toByteArray()

    class DebugInfo : DebuggingSection(".debug_info")
    class DebugLines : DebuggingSection(".debug_line")
    class DebugAbbreviations : DebuggingSection(".debug_abbrev")

    class DebugStrings : DebuggingStringPoolSection(".debug_str")
    class DebugLinesStrings : DebuggingStringPoolSection(".debug_line_str")

    sealed class DebuggingStringPoolSection(name: String) : DebuggingSection(name)
}

class DebuggingSections : Iterable<DebuggingSection> {
    val debugStrings = DebuggingSection.DebugStrings()
    val debugLinesStrings = DebuggingSection.DebugLinesStrings()
    val debugInfo = DebuggingSection.DebugInfo()
    val debugLines = DebuggingSection.DebugLines()
    val debugAbbreviations = DebuggingSection.DebugAbbreviations()

    private val sections = listOf(debugAbbreviations, debugStrings, debugLinesStrings, debugLines, debugInfo)

    override fun iterator(): Iterator<DebuggingSection> = sections.iterator()
}