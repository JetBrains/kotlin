/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.generator

import org.jetbrains.kotlin.fir.checkers.generator.printImports
import org.jetbrains.kotlin.fir.tree.generator.util.writeToFileUsingSmartPrinterIfFileContentChanged
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

private const val CONVERT_ARGUMENT = "convertArgument"

object ArgumentsConverterGenerator {
    fun render(file: File, packageName: String) {
        val convertArgumentFunctionCallConversion = HLFunctionCallConversion(
            "$CONVERT_ARGUMENT({0}, firSymbolBuilder)",
            callType = Any::class.createType(nullable = true)
        )
        val convertersMap = FirToKtConversionCreator.getAllConverters(convertArgumentFunctionCallConversion)
        file.writeToFileUsingSmartPrinterIfFileContentChanged { generate(packageName, convertersMap) }
    }

    private fun SmartPrinter.generate(packageName: String, convertersMap: Map<KClass<*>, HLParameterConversion>) {
        printCopyright()
        println("@file:Suppress(\"UNUSED_PARAMETER\")")
        println()
        println("package $packageName")
        println()
        collectAndPrintImports(convertersMap)
        println()
        printGeneratedMessage()

        generateDispatchingConverter(convertersMap)
        for ((type, converter) in convertersMap) {
            generateSingleConverter(type, converter)
        }
    }

    private fun SmartPrinter.collectAndPrintImports(convertersMap: Map<KClass<*>, HLParameterConversion>) {
        val imports = buildList {
            add("org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder")
            add("org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession")
            convertersMap.values.flatMapTo(this) { it.importsToAdd }
            convertersMap.keys.mapNotNullTo(this) { it.qualifiedName }
        }
        printImports(imports)
    }

    private fun SmartPrinter.generateDispatchingConverter(convertersMap: Map<KClass<*>, HLParameterConversion>) {
        println("internal fun $CONVERT_ARGUMENT(argument: Any?, analysisSession: KtFirAnalysisSession): Any? {")
        withIndent {
            println("return $CONVERT_ARGUMENT(argument, analysisSession.firSymbolBuilder)")
        }
        println("}")
        println()

        println("private fun $CONVERT_ARGUMENT(argument: Any?, firSymbolBuilder: KtSymbolByFirBuilder): Any? {")
        withIndent {
            println("return when (argument) {")
            withIndent {
                println("null -> null")
                for (type in convertersMap.keys) {
                    println("is ${type.typeWithStars} -> $CONVERT_ARGUMENT(argument, firSymbolBuilder)")
                }
                println("else -> argument")
            }
            println("}")
        }
        println("}")
        println()
    }

    private fun SmartPrinter.generateSingleConverter(type: KClass<*>, converter: HLParameterConversion) {
        println("private fun $CONVERT_ARGUMENT(argument: ${type.typeWithStars}, firSymbolBuilder: KtSymbolByFirBuilder): Any? {")
        withIndent {
            println("return ${converter.convertExpression("argument", ConversionContext(currentIndentLengthInUnits, indentUnitLength))}")
        }
        println("}")
        println()
    }

    private val KClass<*>.typeWithStars: String
        get() = buildString {
            append(simpleName)
            if (typeParameters.isNotEmpty()) {
                append(typeParameters.joinToString(", ", "<", ">") { "*" })
            }
        }
}
