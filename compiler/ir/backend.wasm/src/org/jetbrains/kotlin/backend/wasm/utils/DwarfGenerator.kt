/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.utils

import org.jetbrains.kotlin.backend.wasm.dwarf.entries.DebugInformationEntry
import org.jetbrains.kotlin.wasm.ir.debug.DebugData
import org.jetbrains.kotlin.wasm.ir.debug.DebugInformation
import org.jetbrains.kotlin.wasm.ir.debug.DebugInformationGenerator
import org.jetbrains.kotlin.wasm.ir.debug.DebugSection
import org.jetbrains.kotlin.wasm.ir.source.location.SourceLocationMapping

class DwarfGenerator : DebugInformationGenerator {
    override fun addSourceLocation(location: SourceLocationMapping) {
        TODO("Not yet implemented")
    }

    override fun generateDebugInformation(): DebugInformation {
        return listOf(
            DebugSection(
                ".debug_info",
                DebugData.StringData("")
            ),
        )
    }
}