/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf.utils

import org.jetbrains.kotlin.backend.wasm.dwarf.DebuggingSection

abstract class StringTable<I, S : DebuggingSection.DebuggingStringPoolSection> : DebugEntityTable<String, I>() {
    fun write(section: S): List<Int> {
        val offsets = ArrayList<Int>()

        for (string in this) {
            offsets.add(section.offset)
            section.writer.writeBytes(string.toByteArray())
            section.writer.writeByte(0)
        }

        return offsets
    }
}