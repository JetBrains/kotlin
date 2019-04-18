/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("JSStdlibLinker")
package org.jetbrains.kotlin.cli.js.internal

import com.google.gwt.dev.js.ThrowExceptionOnErrorReporter
import org.jetbrains.kotlin.js.backend.JsToStringGenerationVisitor
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.facade.SourceMapBuilderConsumer
import org.jetbrains.kotlin.js.inline.util.fixForwardNameReferences
import org.jetbrains.kotlin.js.parser.parse
import org.jetbrains.kotlin.js.parser.sourcemaps.*
import org.jetbrains.kotlin.js.sourceMap.SourceFilePathResolver
import org.jetbrains.kotlin.js.sourceMap.SourceMap3Builder
import org.jetbrains.kotlin.js.util.TextOutputImpl
import java.io.File
import java.io.StringReader
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val outputFile = File(args[0])
    val baseDir = File(args[1]).canonicalFile
    val wrapperFile = File(args[2])

    val inputPaths = args.drop(3).map { File(it) }
    mergeStdlibParts(outputFile, wrapperFile, baseDir, inputPaths)
}

/**
 * Combines several JS input files, that comprise Kotlin JS Standard Library,
 * into a single JS module.
 * The source maps of these files are combined into a single source map.
 */
private fun mergeStdlibParts(outputFile: File, wrapperFile: File, baseDir: File, inputPaths: List<File>) {
    val program = JsProgram()

    fun File.relativizeIfNecessary(): String = canonicalFile.toRelativeString(baseDir)

    val wrapper = parse(wrapperFile.readText(), ThrowExceptionOnErrorReporter, program.scope, wrapperFile.relativizeIfNecessary())!!
    val insertionPlace = wrapper.createInsertionPlace()

    val allFiles = mutableListOf<File>()
    inputPaths.forEach { collectFiles(it, allFiles) }

    for (file in allFiles) {
        val statements = parse(file.readText(), ThrowExceptionOnErrorReporter, program.scope, file.relativizeIfNecessary())!!
        val block = JsBlock(statements)
        block.fixForwardNameReferences()

        val sourceMapFile = File(file.parent, file.name + ".map")
        if (sourceMapFile.exists()) {
            val sourceMapParse = sourceMapFile.reader().use { SourceMapParser.parse(it) }
            when (sourceMapParse) {
                is SourceMapError -> {
                    System.err.println("Error parsing source map file $sourceMapFile: ${sourceMapParse.message}")
                    exitProcess(1)
                }
                is SourceMapSuccess -> {
                    val sourceMap = sourceMapParse.value
                    val remapper = SourceMapLocationRemapper(sourceMap)
                    remapper.remap(block)
                }
            }
        }

        insertionPlace.statements += statements
    }

    program.globalBlock.statements += wrapper

    val sourceMapFile = File(outputFile.parentFile, outputFile.name + ".map")
    val textOutput = TextOutputImpl()
    val sourceMapBuilder = SourceMap3Builder(outputFile, textOutput, "")
    val consumer = SourceMapBuilderConsumer(File("."), sourceMapBuilder, SourceFilePathResolver(mutableListOf()), true, true)
    program.globalBlock.accept(JsToStringGenerationVisitor(textOutput, consumer))
    val sourceMapContent = sourceMapBuilder.build()

    val programText = textOutput.toString()

    outputFile.writeText(programText + "\n//# sourceMappingURL=${sourceMapFile.name}\n")

    val sourceMapJson = StringReader(sourceMapContent).use { parseJson(it) }
    val sources = (sourceMapJson as JsonObject).properties["sources"] as JsonArray

    sourceMapJson.properties["sourcesContent"] = JsonArray(*sources.elements.map { sourcePath ->
        val sourceFile = File((sourcePath as JsonString).value)
        if (sourceFile.exists()) {
            JsonString(sourceFile.readText())
        } else {
            JsonNull
        }
    }.toTypedArray())

    sourceMapFile.writeText(sourceMapJson.toString())
}

private fun List<JsStatement>.createInsertionPlace(): JsBlock {
    val block = JsGlobalBlock()

    val visitor = object : JsVisitorWithContextImpl() {
        override fun visit(x: JsExpressionStatement, ctx: JsContext<in JsStatement>): Boolean {
            if (isInsertionPlace(x.expression)) {
                ctx.replaceMe(block)
                return false
            } else {
                return super.visit(x, ctx)
            }
        }

        private fun isInsertionPlace(expression: JsExpression): Boolean {
            if (expression !is JsInvocation || !expression.arguments.isEmpty()) return false

            val qualifier = expression.qualifier
            if (qualifier !is JsNameRef || qualifier.qualifier != null) return false
            return qualifier.ident == "insertContent"
        }
    }

    for (statement in this) {
        visitor.accept(statement)
    }
    return block
}

private fun collectFiles(rootFile: File, target: MutableList<File>) {
    if (rootFile.isDirectory) {
        for (child in rootFile.listFiles().sorted()) {
            collectFiles(child, target)
        }
    } else if (rootFile.extension == "js") {
        target += rootFile
    }
}