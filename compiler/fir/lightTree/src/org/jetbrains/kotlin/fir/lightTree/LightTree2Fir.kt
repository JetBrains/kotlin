/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree

import com.intellij.lang.ParserDefinition
import com.intellij.lang.impl.PsiBuilderFactoryImpl
import com.intellij.lexer.Lexer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.CharsetToolkit
import org.jetbrains.kotlin.fir.FirSessionBase
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.parsing.MyKotlinParser
import java.io.File
import java.nio.file.Path

class LightTree2Fir(val stubMode: Boolean, private val parserDefinition: ParserDefinition, private val lexer: Lexer) {
    fun buildFirFile(path: Path): FirFile {
        return buildFirFile(path.toFile())
    }

    fun buildFirFile(file: File): FirFile {
        val text = FileUtil.loadFile(file, CharsetToolkit.UTF8, true).trim()
        return buildFirFile(text, file.name)
    }

    fun buildFirFile(text: String, fileName: String): FirFile {
        val builder = PsiBuilderFactoryImpl().createBuilder(parserDefinition, lexer, text)
        MyKotlinParser.parse(builder)

        return Converter(object : FirSessionBase() {}, stubMode, builder.lightTree).convertFile(builder.lightTree.root, fileName)
    }
}