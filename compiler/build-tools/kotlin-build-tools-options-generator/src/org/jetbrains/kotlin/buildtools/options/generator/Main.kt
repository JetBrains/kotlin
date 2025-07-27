/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.options.generator

import com.squareup.kotlinpoet.TypeName
import org.jetbrains.kotlin.arguments.description.kotlinCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import org.jetbrains.kotlin.utils.addToStdlib.popLast
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.deleteExisting
import kotlin.io.path.isRegularFile
import kotlin.io.path.walk

fun main(args: Array<String>) {
    val genDir = Paths.get(args[0])
    val apiOnly = args[1] == "api"
    val generator = if (apiOnly) {
        BtaApiGenerator()
    } else {
        BtaImplGenerator()
    }
    val levels = mutableListOf<Pair<KotlinCompilerArgumentsLevel, TypeName?>>(kotlinCompilerArguments.topLevel to null)
    val generatedFiles = mutableListOf<Path>()
    while (levels.isNotEmpty()) {
        val level = levels.popLast()
        val output = generator.generateArgumentsForLevel(level.first, level.second)
        output.generatedFiles.forEach { (path, content) ->
            val genFile = genDir.resolve(path)
            GeneratorsFileUtil.writeFileIfContentChanged(genFile.toFile(), content, logNotChanged = false)
            generatedFiles.add(genFile)
        }
        levels += level.first.nestedLevels.map { it to output.argumentTypeName }
    }
    genDir.walk().filter { it.isRegularFile() }.forEach {
        if (it !in generatedFiles) {
            GeneratorsFileUtil.writeFileIfContentChanged(it.toFile(), "", logNotChanged = false)
            it.deleteExisting()
        }
    }
}

interface BtaGenerator {
    fun generateArgumentsForLevel(
        level: KotlinCompilerArgumentsLevel,
        parentClass: TypeName? = null,
        skipXX: Boolean = true,
    ): GeneratorOutputs
}

class GeneratorOutputs(val argumentTypeName: TypeName, val generatedFiles: List<Pair<Path, String>>)
