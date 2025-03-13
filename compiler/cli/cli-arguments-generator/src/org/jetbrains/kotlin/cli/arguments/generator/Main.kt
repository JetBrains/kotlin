/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.arguments.generator

import org.jetbrains.kotlin.arguments.CompilerArgument
import org.jetbrains.kotlin.arguments.CompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.description.Levels
import org.jetbrains.kotlin.arguments.description.kotlinCompilerArguments
import org.jetbrains.kotlin.arguments.types.BooleanType
import org.jetbrains.kotlin.arguments.types.StringArrayType
import org.jetbrains.kotlin.cli.common.arguments.Disables
import org.jetbrains.kotlin.cli.common.arguments.Enables
import org.jetbrains.kotlin.cli.common.arguments.GradleOption
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.withIndent
import java.io.File

fun main(args: Array<String>) {
    generateLevel(Levels.commonToolArguments)
    generateLevel(Levels.commonCompilerArguments)
    generateLevel(Levels.jvmCompilerArguments)
}

private fun generateLevel(levelName: String) {
    val (level, parent) = findLevelWithParent(levelName)
    generateArgumentsClass(level, parent)
}

private fun findLevelWithParent(name: String): Pair<CompilerArgumentsLevel, CompilerArgumentsLevel?> {
    fun find(level: CompilerArgumentsLevel, parent: CompilerArgumentsLevel?): Pair<CompilerArgumentsLevel, CompilerArgumentsLevel?>? {
        if (level.name == name) return level to parent
        return level.nestedLevels.firstNotNullOfOrNull { find(it, level) }
    }
    return find(kotlinCompilerArguments.topLevel, null) ?: error("Level with name $name not found")
}

val levelToClassNameMap = mapOf(
    "commonToolArguments" to "CommonToolArguments2",
    "commonCompilerArguments" to "CommonCompilerArguments2",
    "jvmCompilerArguments" to "K2JVMCompilerArguments2",
)

private fun generateArgumentsClass(
    level: CompilerArgumentsLevel,
    parent: CompilerArgumentsLevel?,
) {
    val genDir = File("/home/demiurg/Programming/kotlin/kotlin/compiler/cli/cli-common/src/org/jetbrains/kotlin/cli/common/arguments").also {
            it.mkdirs()
        }
    genDir.resolve(levelToClassNameMap.getValue(level.name) + ".kt").printWriter().use {
        val printer = Printer(it)
        printer.generateArgumentsClass(level, parent)
    }
}

private fun Printer.generateArgumentsClass(
    level: CompilerArgumentsLevel,
    parent: CompilerArgumentsLevel?,
) {
    println("package org.jetbrains.kotlin.cli.common.arguments")
    println()

    print("open class ${levelToClassNameMap[level.name]}")
    val supertypes = when (parent) {
        null -> "Freezable(), java.io.Serializable"
        else -> "${levelToClassNameMap[parent.name]}()"
    }
    println(" : $supertypes {")
    withIndent {
        for (argument in level.arguments) {
            if (argument.releaseVersionsMetadata.removedVersion != null) continue
            generateAdditionalAnnotations(argument)
            generateArgumentAnnotation(argument)
            generateProperty(argument)
            println()
        }
    }
}

private fun Printer.generateArgumentAnnotation(argument: CompilerArgument) {
    println("@Argument(")
    withIndent {
        println("""value = "-${argument.name}",""")
        argument.shortName?.let { println("""shortName = "$it",""") }
        argument.deprecatedName?.let { println("""shortName = "$it",""") }
        argument.valueDescription.current?.let { println("""valueDescription = "$it",""") }
        val rawDescription = argument.description.current
        val description = if ("\n" in rawDescription) {
            "$tripleQuote$rawDescription$tripleQuote"
        } else {
            "\"$rawDescription\""
        }
        println("description = $description,")
    }
    println(")")
}

private fun Printer.generateAdditionalAnnotations(argument: CompilerArgument) {
    for (annotation in argument.additionalAnnotations) {
        generateAnnotation(annotation)
    }
}

private fun Printer.generateAnnotation(annotation: Annotation) {
    when (annotation) {
        is Enables -> {
            val feature = annotation.feature
            val featureName = feature.name
            println("@Enables(LanguageFeature.$featureName)")
        }
        is Disables -> {
            val feature = annotation.feature
            val featureName = feature.name
            println("@Disables(LanguageFeature.$featureName)")
        }
        is GradleOption -> {
            println("@GradleOption(")
            withIndent {
                println("value = DefaultValue.${annotation.value.name},")
                println("gradleInputType = GradleInputType.${annotation.gradleInputType.name},")
                if (annotation.shouldGenerateDeprecatedKotlinOptions) {
                    println("shouldGenerateDeprecatedKotlinOptions = true,")
                }
                if (annotation.gradleName != "") {
                    println("""gradleName = "${annotation.gradleName}",""")
                }
            }
            println(")")
        }
        else -> error("Unknown annotation ${annotation::class}")
    }
}

private fun Printer.generateProperty(argument: CompilerArgument) {
    val name = argument.name
        .removePrefix("X").removePrefix("X")
        .split("-").joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
        .replaceFirstChar(Char::lowercaseChar)
        .let { it.ifEmpty { "X" } }
    val type = when (argument.valueType) {
        is BooleanType -> "Boolean"
        is StringArrayType -> "Array<String>?"
        else -> "String?"
    }
    println("var $name: $type = ${argument.valueType.defaultValue.current.defaultValueInArgs}")
    withIndent {
        println("set(value) {")
        withIndent {
            println("checkFrozen()")
            if (type == "String?") {
                println("field = if (value.isNullOrEmpty()) null else value")
            } else {
                println("field = value")
            }
        }
        println("}")
    }
}

private val Any?.defaultValueInArgs: Any?
    get() = when (this) {
        is Boolean -> this
        is String -> this
        else -> null
    }



private const val tripleQuote = "\"\"\""
