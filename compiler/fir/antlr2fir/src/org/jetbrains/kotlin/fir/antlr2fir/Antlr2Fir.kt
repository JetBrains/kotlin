/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.antlr2fir

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.jetbrains.kotlin.fir.FirSessionBase
import org.jetbrains.kotlin.fir.antlr2fir.antlr4.generated.KotlinLexer
import org.jetbrains.kotlin.fir.antlr2fir.antlr4.generated.KotlinParser
import org.jetbrains.kotlin.fir.declarations.FirFile
import java.io.File
import java.nio.file.Path

class Antlr2Fir(private val stubMode: Boolean = true) {
    fun buildFirFile(path: Path): FirFile {
        val fileName = path.toString().replaceBeforeLast(File.separator, "").replace(File.separator, "")
        val lexer = KotlinLexer(CharStreams.fromPath(path))
        lexer.removeErrorListeners()
        return buildFirFile(lexer, fileName)
    }

    fun buildFirFile(file: File): FirFile {
        val lexer = KotlinLexer(CharStreams.fromFileName(file.absolutePath))
        lexer.removeErrorListeners()
        return buildFirFile(lexer, file.name)
    }

    fun buildFirFile(input: String): FirFile {
        val lexer = KotlinLexer(CharStreams.fromString(input))
        lexer.removeErrorListeners()
        return buildFirFile(lexer)
    }

    fun buildAntlrOnly(input: String): KotlinParser.KotlinFileContext? {
        val lexer = KotlinLexer(CharStreams.fromString(input))
        val tokens = CommonTokenStream(lexer)
        val parser = KotlinParser(tokens)

        lexer.removeErrorListeners()
        parser.removeErrorListeners()

        return parser.kotlinFile()
    }

    private fun buildFirFile(lexer: KotlinLexer, fileName: String = ""): FirFile {
        val tokens = CommonTokenStream(lexer)
        val parser = KotlinParser(tokens)
        parser.removeErrorListeners()
        // TODO script
        return Antlr2FirBuilder(
            object : FirSessionBase() {},
            stubMode,
            fileName
        ).buildFirFile(parser.kotlinFile())
    }

}