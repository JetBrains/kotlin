/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.base

import kotlin.properties.ReadOnlyProperty

data class KotlinCompilerArgumentsLevel(
    val name: String,
    val arguments: Set<KotlinCompilerArgument>,
    val nestedLevels: Set<KotlinCompilerArgumentsLevel>
) {

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

@KotlinArgumentsDslMarker
internal class KotlinCompilerArgumentsLevelBuilder(
    val name: String
) {
    private val arguments = mutableSetOf<KotlinCompilerArgument>()

    fun compilerArgument(
        config: KotlinCompilerArgumentBuilder.() -> Unit
    ) {
        val argumentBuilder = KotlinCompilerArgumentBuilder()
        config(argumentBuilder)
        arguments.add(argumentBuilder.build())
    }

    fun addCompilerArguments(
        vararg compilerArguments: KotlinCompilerArgument
    ) {
        arguments.addAll(compilerArguments)
    }

    private val nestedLevels = mutableSetOf<KotlinCompilerArgumentsLevel>()

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

    fun build(): KotlinCompilerArgumentsLevel = KotlinCompilerArgumentsLevel(
        name,
        arguments,
        nestedLevels
    )
}

internal fun compilerArgumentsLevel(
    name: String,
    config: KotlinCompilerArgumentsLevelBuilder.() -> Unit
) = ReadOnlyProperty<Any?, KotlinCompilerArgumentsLevel> { _, _ ->
    val levelBuilder = KotlinCompilerArgumentsLevelBuilder(name)
    config(levelBuilder)
    val compilerArgumentsLevel = levelBuilder.build()
    compilerArgumentsLevel
}
