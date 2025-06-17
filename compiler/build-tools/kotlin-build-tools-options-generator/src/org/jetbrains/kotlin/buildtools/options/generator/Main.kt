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

/**
 * Arguments are as follows:
 * 1. &lt;path> - Output directory for generated classes
 * TODO rest
 * 2. &lt;api | impl> - whether the generator should generate API interfaces or implementations
 * 3. (optional) <argumentsLevelName1,argumentsLevelName2> - only generate classes for the specified list of argument level names (see CompilerArgumentsLevelNames.kt)
 */
fun main(args: Array<String>) {
    val genDir = Paths.get(args[0])
    val apiArgsStart = args.indexOf("api").let { if (it == -1) null else it }
    val implArgsStart = args.indexOf("impl").let { if (it == -1) null else it }

    val generatedFiles = mutableListOf<Path>()
    listOfNotNull(
        apiArgsStart?.let { args.copyOfRange(it, implArgsStart ?: args.size) },
        implArgsStart?.let { args.copyOfRange(implArgsStart, args.size) }
    ).map { localArgs ->
        println("localargs ${localArgs.joinToString()}")
        val allowedLevels = if (localArgs[1] == "*") {
            null
        } else {
            localArgs[1].split(",")
        }
        val targetPackage = if (localArgs.size > 2) {
            localArgs[2]
        } else null
        if (localArgs[0] == "api") {
            BtaApiGenerator(targetPackage ?: API_PACKAGE) to allowedLevels
        } else {
            BtaImplGenerator(targetPackage ?: IMPL_PACKAGE) to allowedLevels
        }
    }.forEach { (generator, allowedLevels) ->
        val levels = mutableListOf<Pair<KotlinCompilerArgumentsLevel, TypeName?>>(kotlinCompilerArguments.topLevel to null)
        while (levels.isNotEmpty()) {
            val level = levels.popLast()
            println(generator)
            println("Allowed: $allowedLevels")
            println("Level: " + level.first.name)
            println(allowedLevels)
            if (allowedLevels?.let { level.first.name !in it } ?: false) {
                println("Skipping")
                continue
            }
            val output = generator.generateArgumentsForLevel(level.first, level.second)
            output.generatedFiles.forEach { (path, content) ->
                val genFile = genDir.resolve(path)
                GeneratorsFileUtil.writeFileIfContentChanged(genFile.toFile(), content, logNotChanged = false)
                generatedFiles.add(genFile)
            }
            levels += level.first.nestedLevels.map { it to output.argumentTypeName }
        }
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
