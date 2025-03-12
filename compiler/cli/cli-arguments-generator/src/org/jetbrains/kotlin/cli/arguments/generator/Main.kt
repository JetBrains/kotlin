/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.arguments.generator

import org.jetbrains.kotlin.arguments.CompilerArgument
import org.jetbrains.kotlin.arguments.CompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.description.Levels
import org.jetbrains.kotlin.arguments.description.kotlinCompilerArguments
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.withIndent
import java.io.File

fun main(args: Array<String>) {
    val (level, parent) = findLevelWithParent(Levels.commonToolArguments)
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
    val genDir = File("compiler/cli/cli-common/gen/org/jetbrains/kotlin/cli/common/arguments")
    genDir.resolve(levelToClassNameMap.getValue(level.name) + ".kt").printWriter().use {
        val printer = Printer(it)
        printer.generateArgumentsClass(level, parent)
    }
}

private fun Printer.generateArgumentsClass(
    level: CompilerArgumentsLevel,
    parent: CompilerArgumentsLevel?,
) {
    print("open class ${levelToClassNameMap[level.name]}")
    if (parent != null) {
        print(" : ${levelToClassNameMap[parent.name]}()")
    }
    println(" {")
    withIndent {
        for (argument in level.arguments) {
            if (argument.releaseVersionsMetadata.removedVersion != null) continue
            generateArgumentAnnotation(argument)
            generateAdditionalAnnotations(argument)
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

private fun Printer.generateAdditionalAnnotations(argument: CompilerArgument) {}

private fun Printer.generateProperty(argument: CompilerArgument) {
    val name = argument.name
        .removePrefix("X").removePrefix("X")
        .split("-").joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
        .replaceFirstChar(Char::lowercaseChar)
    println("var $name = ${argument.valueType.defaultValue.current}")
    withIndent {
        println("set(value) {")
        withIndent {
            println("checkFrozen()")
            println("field = value")
        }
        println("}")
    }
}



const val tripleQuote = "\"\"\""
