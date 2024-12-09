/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments

import KotlinArgumentsDslMarker
import kotlinx.serialization.Serializable

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
) : CompilerArgumentsLevelBase

@Serializable
data class CompilerArgumentsTopLevel(
    override val name: String,
    override val arguments: Set<CompilerArgument>,
    override val nestedLevels: Set<CompilerArgumentsLevel>,
    val schemaVersion: Int = 1,
    val releases: List<KotlinReleaseVersionDetails> = KotlinReleaseVersionDetails.allReleaseVersions,
    val types: KotlinArgumentTypes = KotlinArgumentTypes(),
) : CompilerArgumentsLevelBase


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
        config: CompilerArgumentsLevelBuilder.() -> Unit
    ) {
        val levelBuilder = CompilerArgumentsLevelBuilder(name)
        config(levelBuilder)
        _nestedLevels.add(levelBuilder.build())
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

@KotlinArgumentsDslMarker
class CompilerArgumentsTopLevelBuilder(
    name: String
) : CompilerArgumentsLevelBuilderBase(name) {
    fun build(): CompilerArgumentsTopLevel = CompilerArgumentsTopLevel(
        name,
        arguments,
        nestedLevels,
    )
}