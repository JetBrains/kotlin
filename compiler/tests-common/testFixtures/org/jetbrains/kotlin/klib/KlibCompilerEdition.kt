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
    /** Always return [KlibCompilerEdition.CURRENT] */
    NoChange,

    /** Always use the custom compiler ([KlibCompilerEdition.CUSTOM]) to build libraries. */
    CustomCompilerForKlibs {
        override fun getCompilerEditionForKlib(compilerCodename: String?) = KlibCompilerEdition.CUSTOM
    },

    /** Always use the custom compiler ([KlibCompilerEdition.CUSTOM]) to build binaries. */
    CustomCompilerForBinaries {
        override fun getCompilerEditionForBinary() = KlibCompilerEdition.CUSTOM
    },

    @Deprecated("This compiler change scenario is currently not used. It might be removed in the future.")
    BwLatestWithCurrent {
        override fun getCompilerEditionForKlib(compilerCodename: String?) = when (parseCompilerCase(compilerCodename)) {
            CompilerCase.BOTTOM_V1 -> KlibCompilerEdition.CUSTOM
            else -> KlibCompilerEdition.CURRENT
        }
    },

    @Deprecated("This compiler change scenario is currently not used. It might be removed in the future.")
    BwLatestWithLatest {
        override fun getCompilerEditionForKlib(compilerCodename: String?) = when (parseCompilerCase(compilerCodename)) {
            CompilerCase.BOTTOM_V1, CompilerCase.INTERMEDIATE -> KlibCompilerEdition.CUSTOM
            else -> KlibCompilerEdition.CURRENT
        }
    },

    @Deprecated("This compiler change scenario is currently not used. It might be removed in the future.")
    FwLatest {
        override fun getCompilerEditionForKlib(compilerCodename: String?) = when (parseCompilerCase(compilerCodename)) {
            CompilerCase.BOTTOM_V2 -> KlibCompilerEdition.CUSTOM
            else -> KlibCompilerEdition.CURRENT
        }
    };

    /**
     * Get the appropriate [KlibCompilerEdition] for compilation of a KLIB in
     * the current [KlibCompilerChangeScenario] and the specified [compilerCodename].
     *
     * Note: The same [compilerCodename] may resolve to different [KlibCompilerEdition]s in different [KlibCompilerChangeScenario]s.
     */
    open fun getCompilerEditionForKlib(compilerCodename: String?): KlibCompilerEdition = KlibCompilerEdition.CURRENT

    /**
     * Get the appropriate [KlibCompilerEdition] for compilation of a binary in
     * the current [KlibCompilerChangeScenario].
     */
    open fun getCompilerEditionForBinary(): KlibCompilerEdition = KlibCompilerEdition.CURRENT

    companion object {
        private fun parseCompilerCase(compilerCodename: String?): CompilerCase =
            CompilerCase.entries.firstOrNull { it.name == compilerCodename }
                ?: error("Could not resolve ${CompilerCase::class.java} by codename: $compilerCodename")
    }
}
