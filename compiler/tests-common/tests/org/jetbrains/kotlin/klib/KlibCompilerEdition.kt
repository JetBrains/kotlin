/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.klib

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
enum class KlibCompilerChangeScenario(
    val bottomV1: KlibCompilerEdition,
    val bottomV2: KlibCompilerEdition,
    val intermediate: KlibCompilerEdition,
) {
    NoChange(KlibCompilerEdition.CURRENT, KlibCompilerEdition.CURRENT, KlibCompilerEdition.CURRENT),
    BwLatestWithCurrent(KlibCompilerEdition.CUSTOM, KlibCompilerEdition.CURRENT, KlibCompilerEdition.CURRENT),
    BwLatestWithLatest(KlibCompilerEdition.CUSTOM, KlibCompilerEdition.CURRENT, KlibCompilerEdition.CUSTOM),
    FwLatest(KlibCompilerEdition.CURRENT, KlibCompilerEdition.CUSTOM, KlibCompilerEdition.CURRENT);

    override fun toString() = "${this.name}: [$bottomV1 -> $bottomV2, $intermediate]"
}