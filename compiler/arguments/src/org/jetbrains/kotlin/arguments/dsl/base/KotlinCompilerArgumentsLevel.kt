/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.base

import kotlinx.serialization.Serializable
import kotlin.properties.ReadOnlyProperty

/**
 * A Kotlin compiler arguments level.
 *
 * A level represents a collection of compiler arguments and may contain nested levels forming a tree-like structure of arguments.
 * A nested level includes arguments for the parent levels, but not from a sibling level.
 *
 * @param name the name of the level
 * @param arguments a set of [KotlinCompilerArguments][KotlinCompilerArgument] this level contains
 * @param nestedLevels nested levels of compiler arguments
 *
 * Usually compiler argument level should either be defined via top level builder [KotlinCompilerArgumentsBuilder.topLevel]
 * or via special standalone builder DSL - [compilerArgumentsLevel].
 */
@Serializable
data class KotlinCompilerArgumentsLevel(
    val name: String,
    val arguments: Set<KotlinCompilerArgument>,
    val nestedLevels: Set<KotlinCompilerArgumentsLevel>
) {

    /**
     * Merge all arguments and nested levels from [another] level into this one.
     *
     * The [another] [KotlinCompilerArgumentsLevel.name] should be equal to this one!
     * Nested level names may or not be equal to the nested levels of this one.
     * If they are equal, they will be merged as well.
     */
    internal fun mergeWith(another: KotlinCompilerArgumentsLevel): KotlinCompilerArgumentsLevel {
        require(name == another.name) {
            "Names for compiler arguments level should be the same! We are trying to merge $name with ${another.name}"
        }
        val argumentsWithTheSameNames = arguments.map { it.name }.intersect(another.arguments.map { it.name })
        require(argumentsWithTheSameNames.isEmpty()) {
            "Both levels with name $name contain compiler arguments with the same name(s): " +
                    argumentsWithTheSameNames.joinToString()
        }

        val intersectingNestedLevels = nestedLevels.filter { level -> another.nestedLevels.any { level.name == it.name } }.toSet()

        val mergedNestedLevels = nestedLevels.subtract(intersectingNestedLevels) +
                another.nestedLevels.filter { level -> intersectingNestedLevels.none { level.name == it.name } } +
                intersectingNestedLevels.map { level ->
                    level.mergeWith(another.nestedLevels.single { it.name == level.name })
                }
        return KotlinCompilerArgumentsLevel(
            name,
            arguments + another.arguments,
            mergedNestedLevels
        )
    }
}

/**
 * DSL builder for [KotlinCompilerArgumentsLevel].
 */
@KotlinArgumentsDslMarker
internal class KotlinCompilerArgumentsLevelBuilder(
    val name: String
) {
    private val arguments = mutableSetOf<KotlinCompilerArgument>()

    /**
     * Define a new [KotlinCompilerArgument].
     */
    fun compilerArgument(
        config: KotlinCompilerArgumentBuilder.() -> Unit
    ) {
        val argumentBuilder = KotlinCompilerArgumentBuilder()
        config(argumentBuilder)
        arguments.add(argumentBuilder.build())
    }

    /**
     * Add additional [KotlinCompilerArguments][KotlinCompilerArgument] into this level.
     */
    fun addCompilerArguments(
        vararg compilerArguments: KotlinCompilerArgument
    ) {
        arguments.addAll(compilerArguments)
    }

    private val nestedLevels = mutableSetOf<KotlinCompilerArgumentsLevel>()

    /**
     * Define a new nested compiler arguments level.
     */
    fun subLevel(
        name: String,
        mergeWith: Set<KotlinCompilerArgumentsLevel> = emptySet(),
        config: KotlinCompilerArgumentsLevelBuilder.() -> Unit
    ) {
        val levelBuilder = KotlinCompilerArgumentsLevelBuilder(name)
        config(levelBuilder)
        nestedLevels.add(
            mergeWith.fold(levelBuilder.build()) { current, mergingWith ->
                current.mergeWith(mergingWith)
            }
        )
    }

    /**
     * Build a new instance of [KotlinCompilerArgumentsLevel].
     */
    fun build(): KotlinCompilerArgumentsLevel = KotlinCompilerArgumentsLevel(
        name,
        arguments,
        nestedLevels
    )
}

/**
 * Allows creating separated from the main DSL compiler argument level definition, which later could be added into the main DSL.
 *
 * Usage example:
 * ```
 * val deprecatedCommonCompilerArguments by compilerArgumentsLevel {
 *    name = "commonCompilerArguments"
 *
 *    compilerArgument { ... }
 *    compilerArgument { ... }
 * }
 * ```
 *
 * Such standalone compiler argument level could be added into the main definition via [KotlinCompilerArgumentsLevel.mergeWith]
 * method.
 *
 * @see KotlinCompilerArgumentsLevelBuilder
 */
internal fun compilerArgumentsLevel(
    name: String,
    config: KotlinCompilerArgumentsLevelBuilder.() -> Unit
) = ReadOnlyProperty<Any?, KotlinCompilerArgumentsLevel> { _, _ ->
    val levelBuilder = KotlinCompilerArgumentsLevelBuilder(name)
    config(levelBuilder)
    val compilerArgumentsLevel = levelBuilder.build()
    compilerArgumentsLevel
}
