/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf

import org.jetbrains.kotlin.backend.wasm.dwarf.entries.CompileUnit

class UnitTable(private val encoding: Dwarf.Encoding) {
    private val units = mutableListOf<DwarfUnit>()

    inner class DwarfUnit(val unit: CompileUnit) {
        val lineProgram = LineProgram(encoding)
    }
}

