/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree

import com.intellij.lang.impl.PsiBuilderFactoryImpl
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.TestDataPath
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.parsing.KotlinParser
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import java.io.File
import kotlin.system.measureNanoTime

@TestDataPath("\$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners::class)
class LightTree2FirConverterTotalKotlin : AbstractRawFirBuilderTestCase() {
    fun testTotalKotlinLight() {
        val path = System.getProperty("user.dir")
        val root = File(path)
        var counter = 0
        var time = 0L

        val parserDefinition = KotlinParserDefinition()
        val lexer = parserDefinition.createLexer(myProject)
        val lightTreeConverter = LightTree2Fir(true, parserDefinition, lexer)

        println("light test")
        println("BASE PATH: $path")
        for (file in root.walkTopDown()) {
            if (file.isDirectory) continue
            if (file.path.contains("testData") || file.path.contains("resources")) continue
            if (file.extension != "kt") continue

            val text = FileUtil.loadFile(file, CharsetToolkit.UTF8, true).trim()
            time += measureNanoTime {
                val firFile = lightTreeConverter.buildFirFile(text, file.name)
                StringBuilder().also { FirRenderer(it).visitFile(firFile) }.toString()
            }

            counter++
        }
        println("SUCCESS!")
        println("TIME PER FILE: ${(time / counter) * 1e-6} ms, COUNTER: $counter")

    }

    fun testTotalKotlinPsi() {
        val path = System.getProperty("user.dir")
        val root = File(path)
        var counter = 0
        var time = 0L

        println("psi test")
        println("BASE PATH: $path")
        for (file in root.walkTopDown()) {
            if (file.isDirectory) continue
            if (file.path.contains("testData") || file.path.contains("resources")) continue
            if (file.extension != "kt") continue

            val text = FileUtil.loadFile(file, CharsetToolkit.UTF8, true).trim()
            time += measureNanoTime {
                val ktFile = createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(file.path)), text) as KtFile
                val firFile = ktFile.toFirFile(stubMode = true)
                StringBuilder().also { FirRenderer(it).visitFile(firFile) }.toString()
            }

            counter++
        }
        println("SUCCESS!")
        println("TIME PER FILE: ${(time / counter) * 1e-6} ms, COUNTER: $counter")

    }
}