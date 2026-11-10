/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.base

import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.arguments.dsl.types.AllKotlinArgumentTypes
import org.jetbrains.kotlin.arguments.serialization.json.AllDetailsKotlinReleaseVersionSerializer

/**
 * Entry point to all Kotlin compiler arguments.
 *
 * @param schemaVersion generated JSON schema version. Should be increased on any schema change.
 * @param releases all Kotlin release versions. Default is all [KotlinReleaseVersion] entries.
 * @param types all special types used in the argument definitions. Does not include primitive types such as [Boolean] or [String].
 * Default is [AllKotlinArgumentTypes].
 * @param topLevel a root [KotlinCompilerArgumentsLevel].
 *
 * Usually entry point should be created via [compilerArguments] builder.
 */
@Serializable
data class KotlinCompilerArguments(
    // Also update schema changelog in the project Readme.md on schema version bump
    val schemaVersion: Int = 6,
    @Serializable(with = AllDetailsKotlinReleaseVersionSerializer::class)
    val releases: Set<KotlinReleaseVersion> = KotlinReleaseVersion.entries.toSet(),
    val types: AllKotlinArgumentTypes = AllKotlinArgumentTypes(),
    val topLevel: KotlinCompilerArgumentsLevel,
)

/**
 * DSL builder for [KotlinCompilerArguments].
 */
@KotlinArgumentsDslMarker
internal class KotlinCompilerArgumentsBuilder() {
    private lateinit var topLevel: KotlinCompilerArgumentsLevel

    /**
     * Define the root [KotlinCompilerArgumentsLevel] level.
     */
    fun topLevel(
        name: String,
        mergeWith: Set<KotlinCompilerArgumentsLevel> = emptySet(),
        config: KotlinCompilerArgumentsLevelBuilder.() -> Unit
    ) {
        val levelBuilder = KotlinCompilerArgumentsLevelBuilder(name)
        config(levelBuilder)
        topLevel = mergeWith.fold(levelBuilder.build()) { init, level -> init.mergeWith(level) }
    }

    /**
     * Build a new instance of [KotlinCompilerArguments].
     */
    fun build(): KotlinCompilerArguments = KotlinCompilerArguments(
        topLevel = topLevel
    )
}

/**
 * Defines the Kotlin compiler arguments.
 *
 * Usage example:
 * ```
 * val kotlinCompilerArguments = compilerArguments {
 *     topLevel { ... }
 * }
 * ```
 *
 * @see KotlinCompilerArgumentBuilder
 */
internal fun compilerArguments(
    config: KotlinCompilerArgumentsBuilder.() -> Unit,
): KotlinCompilerArguments {
    val kotlinArguments = KotlinCompilerArgumentsBuilder()
    config(kotlinArguments)
    return kotlinArguments.build()
}
