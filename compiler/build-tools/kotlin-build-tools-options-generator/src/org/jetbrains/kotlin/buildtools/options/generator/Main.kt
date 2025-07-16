/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.options.generator

import com.squareup.kotlinpoet.TypeName
import org.jetbrains.kotlin.arguments.description.kotlinCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.utils.addToStdlib.popLast
import java.nio.file.Paths

fun main(args: Array<String>) {
    val genDir = Paths.get(args[0])
    val apiOnly = args[1] == "api"
    val generator = if (apiOnly) {
        BtaApiGenerator(genDir)
    } else {
        BtaImplGenerator(genDir)
    }
    val levels = mutableListOf<Pair<KotlinCompilerArgumentsLevel, TypeName?>>(kotlinCompilerArguments.topLevel to null)
    while (levels.isNotEmpty()) {
        val level = levels.popLast()
        val parentClass = generator.generateArgumentsForLevel(level.first, level.second)
        levels += level.first.nestedLevels.map { it to parentClass }
    }
}

interface BtaGenerator {
    fun generateArgumentsForLevel(
        level: KotlinCompilerArgumentsLevel,
        parentClass: TypeName? = null,
        skipXX: Boolean = true,
    ): TypeName
}