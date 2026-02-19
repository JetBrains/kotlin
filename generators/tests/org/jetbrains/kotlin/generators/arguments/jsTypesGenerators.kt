/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.arguments

import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.Printer
import java.io.File

internal fun generateJsMainFunctionExecutionMode(
    apiDir: File,
    filePrinter: (targetFile: File, Printer.() -> Unit) -> Unit
) {
    val modeFqName = FqName("org.jetbrains.kotlin.gradle.dsl.JsMainFunctionExecutionMode")
    filePrinter(fileFromFqName(apiDir, modeFqName)) {
        generateDeclaration(
            modifiers = "enum class",
            type = modeFqName,
            afterType = "(val mode: String)",
            declarationKDoc = "@param mode",
        ) {
            val modes = hashMapOf(
                K2JsArgumentConstants::CALL.name to K2JsArgumentConstants.CALL,
                K2JsArgumentConstants::NO_CALL.name to K2JsArgumentConstants.NO_CALL
            )

            for ((key, value) in modes) {
                println("/***/")
                println("$key(\"$value\"),")
            }
            println(";")

            println()
            println("/***/")
            println("companion object {")
            withIndent {
                println("/***/")
                println("@JvmStatic")
                println("fun fromMode(mode: String): JsMainFunctionExecutionMode =")
                println("    JsMainFunctionExecutionMode.values().firstOrNull { it.mode == mode }")
                println("        ?: throw IllegalArgumentException(")
                println($$"            \"Unknown main function execution mode: $mode,\\navailable modes: ${JsMainFunctionExecutionMode.values().joinToString { it.mode }}\\n\" +")
                println("                    \"Prefer configuring 'main' value via 'compilerOptions' DSL: https://kotl.in/compiler-options-dsl\"")
                println("        )")
            }
            println("}")
        }
    }
}

internal fun generateJsModuleKind(
    apiDir: File,
    filePrinter: (targetFile: File, Printer.() -> Unit) -> Unit
) {
    val jsModuleKindFqName = FqName("org.jetbrains.kotlin.gradle.dsl.JsModuleKind")
    filePrinter(fileFromFqName(apiDir, jsModuleKindFqName)) {
        generateDeclaration(
            modifiers = "enum class",
            type = jsModuleKindFqName,
            afterType = "(val kind: String)",
            declarationKDoc = "@param kind",
        ) {
            val kinds = hashMapOf(
                K2JsArgumentConstants::MODULE_PLAIN.name to K2JsArgumentConstants.MODULE_PLAIN,
                K2JsArgumentConstants::MODULE_AMD.name to K2JsArgumentConstants.MODULE_AMD,
                K2JsArgumentConstants::MODULE_COMMONJS.name to K2JsArgumentConstants.MODULE_COMMONJS,
                K2JsArgumentConstants::MODULE_UMD.name to K2JsArgumentConstants.MODULE_UMD,
                K2JsArgumentConstants::MODULE_ES.name to K2JsArgumentConstants.MODULE_ES
            )

            for ((key, value) in kinds) {
                println("/***/")
                println("$key(\"$value\"),")
            }
            println(";")

            println()
            println("/***/")
            println("companion object {")
            withIndent {
                println("/***/")
                println("@JvmStatic")
                println("fun fromKind(kind: String): JsModuleKind =")
                println("    JsModuleKind.values().firstOrNull { it.kind == kind }")
                println("        ?: throw IllegalArgumentException(")
                println($$"            \"Unknown JS module kind: $kind,\\navailable kinds: ${JsModuleKind.values().joinToString { it.kind }}\\n\" +")
                println("                    \"Prefer configuring 'moduleKind' value via 'compilerOptions' DSL: https://kotl.in/compiler-options-dsl\"")
                println("        )")
            }
            println("}")
        }
    }
}

internal fun generateJsSourceMapEmbedMode(
    apiDir: File,
    filePrinter: (targetFile: File, Printer.() -> Unit) -> Unit
) {
    val jsSourceMapEmbedKindFqName = FqName("org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode")
    filePrinter(fileFromFqName(apiDir, jsSourceMapEmbedKindFqName)) {
        generateDeclaration(
            modifiers = "enum class",
            type = jsSourceMapEmbedKindFqName,
            afterType = "(val mode: String)",
            declarationKDoc = "@param mode",
        ) {
            val modes = hashMapOf(
                K2JsArgumentConstants::SOURCE_MAP_SOURCE_CONTENT_ALWAYS.name to K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_ALWAYS,
                K2JsArgumentConstants::SOURCE_MAP_SOURCE_CONTENT_NEVER.name to K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_NEVER,
                K2JsArgumentConstants::SOURCE_MAP_SOURCE_CONTENT_INLINING.name to K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_INLINING,
            )

            for ((key, value) in modes) {
                println("/***/")
                println("$key(\"$value\"),")
            }
            println(";")

            println()
            println("/***/")
            println("companion object {")
            withIndent {
                println("/***/")
                println("@JvmStatic")
                println("fun fromMode(mode: String): JsSourceMapEmbedMode =")
                println("    JsSourceMapEmbedMode.values().firstOrNull { it.mode == mode }")
                println("        ?: throw IllegalArgumentException(")
                println($$"            \"Unknown JS source map embed mode: $mode,\\navailable modes: ${JsSourceMapEmbedMode.values().joinToString { it.mode }}\\n\" +")
                println("                    \"Prefer configuring 'sourceMapEmbedSources' value via 'compilerOptions' DSL: https://kotl.in/compiler-options-dsl\"")
                println("        )")
            }
            println("}")
        }
    }
}

internal fun generateJsSourceMapNamesPolicy(
    apiDir: File,
    filePrinter: (targetFile: File, Printer.() -> Unit) -> Unit
) {
    val jsSourceMapNamesPolicyFqName = FqName("org.jetbrains.kotlin.gradle.dsl.JsSourceMapNamesPolicy")
    filePrinter(fileFromFqName(apiDir, jsSourceMapNamesPolicyFqName)) {
        generateDeclaration(
            modifiers = "enum class",
            type = jsSourceMapNamesPolicyFqName,
            afterType = "(val policy: String)",
            declarationKDoc = "@param policy",
        ) {
            val modes = hashMapOf(
                K2JsArgumentConstants::SOURCE_MAP_NAMES_POLICY_NO.name to K2JsArgumentConstants.SOURCE_MAP_NAMES_POLICY_NO,
                K2JsArgumentConstants::SOURCE_MAP_NAMES_POLICY_SIMPLE_NAMES.name to K2JsArgumentConstants.SOURCE_MAP_NAMES_POLICY_SIMPLE_NAMES,
                K2JsArgumentConstants::SOURCE_MAP_NAMES_POLICY_FQ_NAMES.name to K2JsArgumentConstants.SOURCE_MAP_NAMES_POLICY_FQ_NAMES,
            )

            for ((key, value) in modes) {
                println("/***/")
                println("$key(\"$value\"),")
            }
            println(";")

            println()
            println("/***/")
            println("companion object {")
            withIndent {
                println("/***/")
                println("@JvmStatic")
                println("fun fromPolicy(policy: String): JsSourceMapNamesPolicy =")
                println("    JsSourceMapNamesPolicy.values().firstOrNull { it.policy == policy }")
                println("        ?: throw IllegalArgumentException(")
                println($$"            \"Unknown JS source map names policy: $policy,\\navailable policies: ${JsSourceMapNamesPolicy.values().joinToString{ it.policy }}\\n\" +")
                println("                    \"Prefer configuring 'sourceMapNamesPolicy' value via 'compilerOptions' DSL: https://kotl.in/compiler-options-dsl\"")
                println("        )")
            }
            println("}")
        }
    }
}
