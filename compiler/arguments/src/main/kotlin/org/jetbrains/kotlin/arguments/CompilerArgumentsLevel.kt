/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments

import KotlinArgumentsDslMarker
import kotlinx.serialization.Serializable
import kotlin.properties.ReadOnlyProperty

interface CompilerArgumentsLevelBase {
    val name: String
    val arguments: Set<CompilerArgument>
    val nestedLevels: Set<CompilerArgumentsLevel>
}

@Serializable
data class CompilerArgumentsLevel(
    override val name: String,
    override val arguments: Set<CompilerArgument>,
    override val nestedLevels: Set<CompilerArgumentsLevel>
) : CompilerArgumentsLevelBase {

    fun mergeWith(another: CompilerArgumentsLevel): CompilerArgumentsLevel {
        require(name == another.name) {
            "Names for compiler arguments level should be the same! We are trying to merge $name with ${another.name}"
        }

        val intersectingNestedLevels = nestedLevels.filter { level -> another.nestedLevels.any { level.name == it.name } }.toSet()

        val mergedNestedLevels = nestedLevels.subtract(intersectingNestedLevels) +
                another.nestedLevels.filter { level -> intersectingNestedLevels.none { level.name == it.name } } +
                intersectingNestedLevels.map { level ->
                    level.mergeWith(another.nestedLevels.single { it.name == level.name} )
                }
        return CompilerArgumentsLevel(
            name,
            arguments + another.arguments,
            mergedNestedLevels
        )
    }
}

@Serializable
data class CompilerArguments(
    val schemaVersion: Int = 1,
    @Serializable(with = AllDetailsKotlinReleaseVersionSerializer::class)
    val releases: Set<KotlinReleaseVersion> = KotlinReleaseVersions.allKotlinReleaseVersions,
    val types: KotlinArgumentTypes = KotlinArgumentTypes(),
    val topLevel: CompilerArgumentsLevel,
)


abstract class CompilerArgumentsLevelBuilderBase(
    override val name: String
) : CompilerArgumentsLevelBase {
    private val _arguments = mutableSetOf<CompilerArgument>()
    override val arguments: Set<CompilerArgument>
        get() = _arguments

    fun compilerArgument(
        config: CompilerArgumentBuilder.() -> Unit
    ) {
        val argumentBuilder = CompilerArgumentBuilder()
        config(argumentBuilder)
        _arguments.add(argumentBuilder.build())
    }

    private val _nestedLevels = mutableSetOf<CompilerArgumentsLevel>()
    override val nestedLevels: Set<CompilerArgumentsLevel>
        get() = _nestedLevels

    fun subLevel(
        name: String,
        mergeWith: Set<CompilerArgumentsLevel> = emptySet(),
        config: CompilerArgumentsLevelBuilder.() -> Unit
    ) {
        val levelBuilder = CompilerArgumentsLevelBuilder(name)
        config(levelBuilder)
        _nestedLevels.add(
            mergeWith.fold(levelBuilder.build()) { current, mergingWith ->
                current.mergeWith(mergingWith)
            }
        )
    }
}

@KotlinArgumentsDslMarker
class CompilerArgumentsLevelBuilder(
    name: String
) : CompilerArgumentsLevelBuilderBase(name) {
    fun build(): CompilerArgumentsLevel = CompilerArgumentsLevel(
        name,
        arguments,
        nestedLevels
    )
}

fun compilerArgumentsLevel(
    name: String,
    config: CompilerArgumentsLevelBuilder.() -> Unit
) = ReadOnlyProperty<Any?, CompilerArgumentsLevel> { _, _ ->
    val levelBuilder = CompilerArgumentsLevelBuilder(name)
    config(levelBuilder)
    val compilerArgumentsLevel = levelBuilder.build()
    compilerArgumentsLevel
}

@KotlinArgumentsDslMarker
class CompilerArgumentsBuilder() {
    private lateinit var topLevel: CompilerArgumentsLevel

    fun topLevel(
        name: String,
        mergeWith: Set<CompilerArgumentsLevel> = emptySet(),
        config: CompilerArgumentsLevelBuilder.() -> Unit
    ) {
        val levelBuilder = CompilerArgumentsLevelBuilder(name)
        config(levelBuilder)
        topLevel = mergeWith.fold(levelBuilder.build()) { init, level -> init.mergeWith(level) }
    }

    fun build(): CompilerArguments = CompilerArguments(
        topLevel = topLevel
    )
}