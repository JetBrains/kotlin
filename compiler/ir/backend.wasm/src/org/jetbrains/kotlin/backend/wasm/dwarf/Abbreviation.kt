/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf

import org.jetbrains.kotlin.backend.wasm.dwarf.entries.DwAttribute
import org.jetbrains.kotlin.backend.wasm.dwarf.entries.DwTag

class Abbreviation(
    val tag: DwTag,
    val hasChildren: Boolean,
    val attributes: List<Specification>,
) {
    fun write(section: DebuggingSection.DebugAbbreviations) {
        section.writer.writeVarUInt32(tag.value)
        section.writer.writeVarUInt1(hasChildren)
        for (attribute in attributes) {
            attribute.write(section)
        }
        // Null name and form
        section.writer.writeUByte(0u)
        section.writer.writeUByte(0u)
    }

    data class Specification(val attribute: DwAttribute, val form: DwForm) {
        fun write(section: DebuggingSection.DebugAbbreviations) {
            section.writer.writeVarUInt32(attribute.opcode)
            section.writer.writeVarUInt32(form.opcode)
        }
    }
}

infix fun DwAttribute.by(form: DwForm): Abbreviation.Specification =
    Abbreviation.Specification(this, form)
