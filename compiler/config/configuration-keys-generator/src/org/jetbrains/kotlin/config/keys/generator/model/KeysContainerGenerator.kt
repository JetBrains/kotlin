/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.keys.generator.model

import org.jetbrains.kotlin.fir.tree.generator.util.writeToFileUsingSmartPrinterIfFileContentChanged
import org.jetbrains.kotlin.generators.util.inBracketsWithIndent
import org.jetbrains.kotlin.generators.util.printCopyright
import org.jetbrains.kotlin.generators.util.printGeneratedMessage
import org.jetbrains.kotlin.generators.util.printImports
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.withIndent
import java.io.File

object KeysContainerGenerator {
    private val defaultImports = listOf(
        "org.jetbrains.kotlin.config.CompilerConfigurationKey",
        "org.jetbrains.kotlin.config.CompilerConfiguration"
    )

    fun generate(file: File, container: KeysContainer) {
        file.writeToFileUsingSmartPrinterIfFileContentChanged {
            printCopyright()
            println("""@file:Suppress("IncorrectFormatting", "unused")""")
            println()
            println("package ${container.packageName}\n")
            printGeneratedMessage()
            collectAndPrintImports(container)
            generateKeysContainingClass(container)
            generateKeysAccessors(container)
        }
    }

    private fun SmartPrinter.collectAndPrintImports(container: KeysContainer) {
        printImports(
            container.packageName,
            importableTypes = container.keys.flatMap { it.types },
            simpleImports = defaultImports + container.keys.flatMap { it.importsToAdd },
            starImports = emptyList(),
        )
    }

    private fun SmartPrinter.generateKeysContainingClass(container: KeysContainer) {
        inBracketsWithIndent("object ${container.className}") {
            for (key in container.keys) {
                key.comment?.let {
                    println("// $it")
                }
                when (key) {
                    is DeprecatedKey -> generateDeprecatedKey(key)
                    else -> generateRegularKey(key)
                }
                println()
            }
        }
        println()
    }

    private fun SmartPrinter.generateRegularKey(key: Key) {
        println("@JvmField")
        println("val ${key.name} = CompilerConfigurationKey.create<${key.typeString}>(\"${key.description}\")")
    }

    private fun SmartPrinter.generateDeprecatedKey(key: DeprecatedKey) {
        val deprecation = key.deprecation
        println("@Deprecated(")
        withIndent {
            println("\"${deprecation.message}\",")
            if (deprecation.replaceWith.expression.isNotEmpty()) {
                print("ReplaceWith(\"${deprecation.replaceWith.expression}\"")
                if (deprecation.replaceWith.imports.isNotEmpty()) {
                    print(deprecation.replaceWith.imports.joinToString(", ", prefix = ", ") { "\"$it\""})
                }
                println("),")
            }
            println("DeprecationLevel.${deprecation.level.name},")
        }
        println(")")
        println("@JvmField")
        println("val ${key.name} = ${key.initializer}")
    }

    private fun SmartPrinter.generateKeysAccessors(container: KeysContainer) {
        for (key in container.keys) {
            when (key) {
                is SimpleKey -> generateSimpleKeyAccessors(container, key)
                is CollectionKey -> generateCollectionKeyAccessors(container, key)
                is DeprecatedKey -> {}
            }
        }
    }

    private fun SmartPrinter.generateSimpleKeyAccessors(container: KeysContainer, key: SimpleKey) {
        val booleanFlag = key.typeString == "Boolean"
        val nullable = !booleanFlag && key.defaultValue == null
        val returnType = key.typeString.applyIf(nullable) { "$this?"}

        println("var CompilerConfiguration.${key.accessorName}: $returnType")
        val keyAccess = container.keyAccessString(key)
        withIndent {
            val getterBody = when {
                booleanFlag -> "getBoolean($keyAccess)"
                key.defaultValue != null -> "get($keyAccess, ${key.defaultValue})"
                else -> "get($keyAccess)"
            }
            println("get() = $getterBody")
            val valueForPut = when {
                nullable -> "requireNotNull(value) { \"nullable values are not allowed\" }"
                else -> "value"
            }
            println("set(value) { put($keyAccess, $valueForPut) }")
        }
        println()
    }

    private fun SmartPrinter.generateCollectionKeyAccessors(container: KeysContainer, key: CollectionKey) {
        println("var CompilerConfiguration.${key.accessorName}: ${key.mutableTypeString}")
        val keyAccess = container.keyAccessString(key)
        withIndent {
            val getterFunction = when (key) {
                is ListKey -> "getList"
                is MapKey -> "getMap"
            }
            println("get() = $getterFunction($keyAccess)")
            println("set(value) { put($keyAccess, value) }")
        }
        println()
    }

    private fun KeysContainer.keyAccessString(key: Key): String {
        return "${this.className}.${key.name}"
    }
}
