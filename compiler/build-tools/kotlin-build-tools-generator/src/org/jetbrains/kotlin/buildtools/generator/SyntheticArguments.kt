/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.generator

import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames
import org.jetbrains.kotlin.arguments.description.kotlinCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerPhase

internal class SyntheticArgumentInterface(
    val name: String,
    val level: KotlinCompilerArgumentsLevel,
    val parentInterfaces: List<SyntheticArgumentInterface>,
    val concreteClassName: String? = null,
    val restrictedToCompilerPhase: KotlinCompilerPhase? = null,
)

private fun SyntheticArgumentInterface.toLevel(): KotlinCompilerArgumentsLevel =
    KotlinCompilerArgumentsLevel(
        name,
        level.arguments.filter { it.restrictedToCompilerPhase == restrictedToCompilerPhase }.toSet(),
        if (syntheticArgumentInterfaces.any { this in it.parentInterfaces }) {
            setOf(DummyLevel)
        } else emptySet(),
        level.modifiers
    )

val DummyLevel = KotlinCompilerArgumentsLevel("", emptySet(), emptySet(), emptySet())

/**
 * Looks up the merged argument level (actual + removed arguments) by name from [kotlinCompilerArguments].
 *
 * The synthetic KLIB-based interfaces must use the merged level rather than the raw `actual*Arguments`
 * objects, otherwise removed arguments (declared in the separate `removed*Arguments` levels and combined
 * only via `mergeWith` in `compilerArguments.kt`) would never reach the API generator.
 */
private fun findMergedLevel(name: String): KotlinCompilerArgumentsLevel {
    fun search(level: KotlinCompilerArgumentsLevel): KotlinCompilerArgumentsLevel? {
        if (level.name == name) return level
        for (nested in level.nestedLevels) {
            search(nested)?.let { return it }
        }
        return null
    }
    return search(kotlinCompilerArguments.topLevel) ?: error("Merged level $name is not found in kotlinCompilerArguments")
}

object SyntheticArgumentNames {
    const val commonKlibBasedArguments = "CommonKlibBasedArguments"
    const val commonKlibBasedArgumentsKlibArguments = "CommonKlibBasedArgumentsKlibArguments"
    const val commonKlibBasedArgumentsLinkingArguments = "CommonKlibBasedArgumentsLinkingArguments"
    const val commonJsAndWasmArguments = "CommonJsAndWasmArguments"
    const val commonJsAndWasmCompilerKlibArguments = "CommonJsAndWasmCompilerKlibArguments"
    const val commonJsAndWasmCompilerLinkingArguments = "CommonJsAndWasmCompilerLinkingArguments"
    const val jsCompilerArguments = "JsCompilerArguments"
    const val jsCompilerKlibArguments = "JsCompilerKlibArguments"
    const val jsCompilerLinkingArguments = "JsCompilerLinkingArguments"
    const val wasmCompilerArguments = "WasmCompilerArguments"
    const val wasmCompilerKlibArguments = "WasmCompilerKlibArguments"
    const val wasmCompilerLinkingArguments = "WasmCompilerLinkingArguments"
}

internal val syntheticArgumentInterfaces = buildList {
    val commonKlibBasedArguments = findMergedLevel(CompilerArgumentsLevelNames.commonKlibBasedArguments)
    val commonJsAndWasmArguments = findMergedLevel(CompilerArgumentsLevelNames.commonJsAndWasmArguments)
    val jsArguments = findMergedLevel(CompilerArgumentsLevelNames.jsArguments)
    val wasmArguments = findMergedLevel(CompilerArgumentsLevelNames.wasmArguments)

    val commonKlibBasedCompilerArguments = SyntheticArgumentInterface(
        SyntheticArgumentNames.commonKlibBasedArguments,
        commonKlibBasedArguments,
        emptyList(),
        "CommonKlibBasedArgumentsImpl",
    ).also(::add)
    val commonKlibBasedCompilerKlibArguments = SyntheticArgumentInterface(
        SyntheticArgumentNames.commonKlibBasedArgumentsKlibArguments,
        commonKlibBasedArguments,
        listOf(commonKlibBasedCompilerArguments),
        "CommonKlibBasedArgumentsImpl",
        KotlinCompilerPhase.KLIB_COMPILATION
    ).also(::add)
    val commonKlibBasedCompilerLinkingArguments = SyntheticArgumentInterface(
        SyntheticArgumentNames.commonKlibBasedArgumentsLinkingArguments,
        commonKlibBasedArguments,
        listOf(commonKlibBasedCompilerArguments),
        "CommonKlibBasedArgumentsImpl",
        KotlinCompilerPhase.BACKEND_COMPILATION
    ).also(::add)

    val commonJsAndWasmCompilerArguments = SyntheticArgumentInterface(
        SyntheticArgumentNames.commonJsAndWasmArguments,
        commonJsAndWasmArguments,
        listOf(commonKlibBasedCompilerArguments),
        "CommonJsAndWasmArgumentsImpl",
    ).also(::add)

    val commonJsAndWasmCompilerKlibArguments = SyntheticArgumentInterface(
        SyntheticArgumentNames.commonJsAndWasmCompilerKlibArguments,
        commonJsAndWasmArguments,
        listOf(commonJsAndWasmCompilerArguments, commonKlibBasedCompilerKlibArguments),
        "CommonJsAndWasmArgumentsImpl",
        KotlinCompilerPhase.KLIB_COMPILATION
    ).also(::add)

    val commonJsAndWasmCompilerLinkingArguments = SyntheticArgumentInterface(
        SyntheticArgumentNames.commonJsAndWasmCompilerLinkingArguments,
        commonJsAndWasmArguments,
        listOf(commonJsAndWasmCompilerArguments, commonKlibBasedCompilerLinkingArguments),
        "CommonJsAndWasmArgumentsImpl",
        KotlinCompilerPhase.BACKEND_COMPILATION
    ).also(::add)

    val jsCompilerArguments = SyntheticArgumentInterface(
        SyntheticArgumentNames.jsCompilerArguments,
        jsArguments,
        listOf(commonJsAndWasmCompilerArguments),
        "JsArgumentsImpl",
    ).also(::add)

    SyntheticArgumentInterface(
        SyntheticArgumentNames.jsCompilerKlibArguments,
        jsArguments,
        listOf(jsCompilerArguments, commonJsAndWasmCompilerKlibArguments),
        "JsArgumentsImpl",
        KotlinCompilerPhase.KLIB_COMPILATION
    ).also(::add)

    SyntheticArgumentInterface(
        SyntheticArgumentNames.jsCompilerLinkingArguments,
        jsArguments,
        listOf(jsCompilerArguments, commonJsAndWasmCompilerLinkingArguments),
        "JsArgumentsImpl",
        KotlinCompilerPhase.BACKEND_COMPILATION
    ).also(::add)

    val wasmCompilerArguments = SyntheticArgumentInterface(
        SyntheticArgumentNames.wasmCompilerArguments,
        wasmArguments,
        listOf(commonJsAndWasmCompilerArguments),
        "WasmArgumentsImpl",
    ).also(::add)

    SyntheticArgumentInterface(
        SyntheticArgumentNames.wasmCompilerKlibArguments,
        wasmArguments,
        listOf(wasmCompilerArguments, commonJsAndWasmCompilerKlibArguments),
        "WasmArgumentsImpl",
        KotlinCompilerPhase.KLIB_COMPILATION
    ).also(::add)

    SyntheticArgumentInterface(
        SyntheticArgumentNames.wasmCompilerLinkingArguments,
        wasmArguments,
        listOf(wasmCompilerArguments, commonJsAndWasmCompilerLinkingArguments),
        "WasmArgumentsImpl",
        KotlinCompilerPhase.BACKEND_COMPILATION
    ).also(::add)
}

internal val syntheticLevels = syntheticArgumentInterfaces.associate { it.name to it.toLevel() }
