/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.backend

import org.jetbrains.kotlin.bir.symbols.BirSymbol

class TemporaryVariablesScope(val scopeOwnerSymbol: BirSymbol) {
    private var lastTemporaryIndex: Int = 0
    private fun nextTemporaryIndex(): Int = lastTemporaryIndex++

    fun inventNameForTemporary(prefix: String = "tmp", nameHint: String? = null): String {
        val index = nextTemporaryIndex()
        return if (nameHint != null) "$prefix${index}_$nameHint" else "$prefix$index"
    }
}