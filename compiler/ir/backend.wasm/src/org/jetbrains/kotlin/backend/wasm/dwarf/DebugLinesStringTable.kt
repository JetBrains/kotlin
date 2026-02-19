/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf

import org.jetbrains.kotlin.backend.wasm.dwarf.utils.StringTable

class DebugLinesStringTable : StringTable<DebugLinesStringTable.StringRef, DebuggingSection.DebugLinesStrings>() {
    override fun computeId(index: Int) = StringRef(index + 1)

    @JvmInline
    value class StringRef(val index: Int)
}