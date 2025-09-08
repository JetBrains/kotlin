/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

enum class KlibIrInlinerMode(val state: String) {
    DEFAULT("default"),
    DISABLED("disabled"),
    FULL("full"),
    INTRA_MODULE("intra-module");

    companion object {
        fun fromString(string: String): KlibIrInlinerMode? = KlibIrInlinerMode.entries.find { it.state == string }

        fun availableValues() = KlibIrInlinerMode.entries.joinToString(prefix = "{", postfix = "}") { it.state }
    }
}