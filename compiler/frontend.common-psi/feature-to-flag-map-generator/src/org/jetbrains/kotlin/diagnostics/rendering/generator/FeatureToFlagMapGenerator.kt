/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.rendering.generator

import org.jetbrains.kotlin.arguments.description.CompilerArgumentsLevelNames
import org.jetbrains.kotlin.arguments.description.kotlinCompilerArguments
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.cli.common.arguments.Enables
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import java.io.File

private val COPYRIGHT by lazy { File("license/COPYRIGHT_HEADER.txt").readText() }
private const val ORIGIN_FILE_PATH = "compiler/arguments/src/org/jetbrains/kotlin/arguments/description"
private const val GENERATED_PACKAGE = "org.jetbrains.kotlin.diagnostics.rendering"
private const val GENERATED_FILE_NAME = "FeatureToFlagMapGenerated.kt"
private const val GENERATOR_FILE_NAME = "FeatureToFlagMapGenerator.kt" // this file

fun main(args: Array<String>) {
    val genDir = File(args[0])
    val outputFile = genDir.resolve(GENERATED_PACKAGE.replace('.', '/')).resolve(GENERATED_FILE_NAME)

    val commonLevel = findCommonCompilerArgumentsLevel()
        ?: error("Level '${CompilerArgumentsLevelNames.commonCompilerArguments}' not found")

    val featureToFlag = buildFeatureToFlagMap(commonLevel)

    val fileText = buildString {
        with(SmartPrinter(this)) {
            println(COPYRIGHT)
            println("package $GENERATED_PACKAGE")
            println()
            println("import org.jetbrains.kotlin.config.LanguageFeature")
            println()
            print(GeneratorsFileUtil.GENERATED_MESSAGE_PREFIX)
            println(GENERATOR_FILE_NAME)
            println("// Please declare arguments in $ORIGIN_FILE_PATH/CommonCompilerArguments.kt")
            println(GeneratorsFileUtil.GENERATED_MESSAGE_SUFFIX)
            println()
            // must be public, used by IDE plugin
            println("val featureToEnablingFlagMap: Map<LanguageFeature, String> = mapOf(")
            withIndent {
                for ([feature, flag] in featureToFlag) {
                    println("LanguageFeature.${feature.name} to \"$flag\",")
                }
            }
            println(")")
        }
    }

    GeneratorsFileUtil.writeFileIfContentChanged(outputFile, fileText, logNotChanged = false)
}

private fun findCommonCompilerArgumentsLevel(): KotlinCompilerArgumentsLevel? {
    val name = CompilerArgumentsLevelNames.commonCompilerArguments
    fun find(level: KotlinCompilerArgumentsLevel): KotlinCompilerArgumentsLevel? {
        if (level.name == name) return level
        return level.nestedLevels.firstNotNullOfOrNull { find(it) }
    }
    return find(kotlinCompilerArguments.topLevel)
}

private data class FeatureAndValue(val feature: LanguageFeature, val value: String)

private fun buildFeatureToFlagMap(level: KotlinCompilerArgumentsLevel): Map<LanguageFeature, String> {
    data class ArgumentAndValue(val argument: String, val value: String)

    return level.arguments
        .flatMap { argument ->
            val flag = "-${argument.name}"
            argument.additionalAnnotations
                .filterIsInstance<Enables>()
                .map { FeatureAndValue(it.feature, it.ifValueIs) to flag }
        }
        .groupBy(
            keySelector = { it.first.feature },
            valueTransform = { ArgumentAndValue(argument = it.second, value = it.first.value) },
        )
        .mapValues { [_, values] ->
            val argument = values.first().argument
            if (values.any { it.value.isNotEmpty() }) {
                buildString {
                    append(argument)
                    append('=')
                    if (values.size == 1) {
                        append(values.first().value)
                    } else {
                        values.joinTo(buffer = this, separator = "|", prefix = "{", postfix = "}") { it.value }
                    }
                }
            } else {
                argument
            }
        }
        .toList()
        .sortedBy { it.first.name }
        .toMap()
}
