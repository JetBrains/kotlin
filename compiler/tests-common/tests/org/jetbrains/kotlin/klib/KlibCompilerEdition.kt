/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.klib

import org.jetbrains.kotlin.codegen.ModuleInfo.CompilerCase

/**
 * At the moment, only two compiler editions are supported:
 * [CURRENT] - the compiler that was just built from fresh sources.
 * [CUSTOM] - a custom (previously built and published) compiler.
 */
enum class KlibCompilerEdition(
    val args: List<String> = emptyList(),
) {
    CURRENT,
    CUSTOM,
}

/**
 *      Intermediate -> Bottom
 *          \           /
 *              Main
 *
 * For `Bw*` = `Backward*` cases with check that we can replace klib that built with a newer compiler version in runtime
 * and it works.
 * For `Fw*` = `Forward*` cases we check that the klib built with older compiler can be used in runtime.
 */
enum class KlibCompilerChangeScenario {
    NoChange {
        override fun get(alias: String) = KlibCompilerEdition.CURRENT
    },

    BwLatestWithCurrent {
        override fun get(alias: String) = when (CompilerCase.valueOf(alias)) {
            CompilerCase.BOTTOM_V1 -> KlibCompilerEdition.CUSTOM
            else -> KlibCompilerEdition.CURRENT
        }
    },

    BwLatestWithLatest {
        override fun get(alias: String) = when (CompilerCase.valueOf(alias)) {
            CompilerCase.BOTTOM_V1, CompilerCase.INTERMEDIATE -> KlibCompilerEdition.CUSTOM
            else -> KlibCompilerEdition.CURRENT
        }
    },

    FwLatest {
        override fun get(alias: String) = when (CompilerCase.valueOf(alias)) {
            CompilerCase.BOTTOM_V2 -> KlibCompilerEdition.CUSTOM
            else -> KlibCompilerEdition.CURRENT
        }
    };

    abstract operator fun get(alias: String): KlibCompilerEdition
}
