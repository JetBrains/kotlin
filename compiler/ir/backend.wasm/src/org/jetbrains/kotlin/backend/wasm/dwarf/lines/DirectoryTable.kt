/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf.lines

import org.jetbrains.kotlin.backend.wasm.dwarf.DebugLinesStringTable
import org.jetbrains.kotlin.backend.wasm.dwarf.utils.DebugEntityTable

@JvmInline
value class DirectoryId(val index: Int)

class DirectoryTable : DebugEntityTable<DebugLinesStringTable.StringRef, DirectoryId>() {
    override fun computeId(index: Int) = DirectoryId(index)
}

