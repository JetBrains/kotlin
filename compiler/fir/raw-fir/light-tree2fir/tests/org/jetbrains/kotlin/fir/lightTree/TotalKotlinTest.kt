/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.impl.DebugUtil
import com.intellij.testFramework.TestDataPath
import com.intellij.util.PathUtil
import org.jetbrains.kotlin.KtIoFileSourceFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.KtSourceFileLinesMapping
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.fir.builder.StubFirScopeProvider
import org.jetbrains.kotlin.fir.session.FirSessionFactory
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.readSourceFileWithMapping
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import java.io.File
import kotlin.system.measureNanoTime

@TestDataPath("/")
@RunWith(JUnit3RunnerWithInners::class)
class TotalKotlinTest : AbstractRawFirBuilderTestCase() {
    private fun generateFirFromPsi(onlyPsi: Boolean, text: String, path: String) {
        val ktFile = createPsiFile(FileUtil.getNameWithoutExtension(PathUtil.getFileName(path)), text) as KtFile
        if (onlyPsi) {
            DebugUtil.psiTreeToString(ktFile, false)
        } else {
            val firFile = ktFile.toFirFile()
            StringBuilder().also { FirRenderer(it).visitFile(firFile) }.toString()
        }
    }

    private fun generateFirFromLightTree(
        onlyLightTree: Boolean, converter: LightTree2Fir,
        text: CharSequence, sourceFile: KtSourceFile, linesMapping: KtSourceFileLinesMapping
    ) {
        if (onlyLightTree) {
            val lightTree = LightTree2Fir.buildLightTree(text)
            DebugUtil.lightTreeToString(lightTree, false)
        } else {
            val firFile = converter.buildFirFile(text, sourceFile, linesMapping)
            StringBuilder().also { FirRenderer(it).visitFile(firFile) }.toString()
        }
    }

    private fun totalKotlinLight(onlyLightTree: Boolean) {
        val path = System.getProperty("user.dir")
        var counter = 0
        var time = 0L

        val lightTreeConverter = LightTree2Fir(
            session = FirSessionFactory.createEmptySession(),
            scopeProvider = StubFirScopeProvider,
            diagnosticsReporter = null
        )

        if (onlyLightTree) println("LightTree generation") else println("Fir from LightTree converter")
        println("BASE PATH: $path")
        path.walkTopDown {
            val sourceFile = KtIoFileSourceFile(it)
            val (code, linesMapping) = with(it.inputStream().reader(Charsets.UTF_8)) {
                this.readSourceFileWithMapping()
            }
            time += measureNanoTime {
                generateFirFromLightTree(onlyLightTree, lightTreeConverter, code, sourceFile, linesMapping)
            }

            counter++
        }
        println("SUCCESS!")
        println("TIME PER FILE: ${(time / counter) * 1e-6} ms, COUNTER: $counter")

    }

    private fun totalKotlinPsi(onlyPsi: Boolean) {
        val path = System.getProperty("user.dir")
        val root = File(path)
        var counter = 0
        var time = 0L

        if (onlyPsi) println("Psi generation") else println("Fir from Psi converter")
        println("BASE PATH: $path")
        for (file in root.walkTopDown()) {
            if (file.isDirectory) continue
            /* TODO: fix this, please !!! */
            if (file.path.contains("kotlin-native") ||
                file.path.lowercase().contains("testdata") ||
                file.path.contains("resources")
            ) continue
            if (file.extension != "kt") continue

            val text = FileUtil.loadFile(file, CharsetToolkit.UTF8, true).trim()
            time += measureNanoTime {
                try {
                    generateFirFromPsi(onlyPsi, text, file.path)
                } catch (e: Exception) {
                    throw IllegalStateException(file.path, e)
                }
            }

            counter++
        }
        println("SUCCESS!")
        println("TIME PER FILE: ${(time / counter) * 1e-6} ms, COUNTER: $counter")

    }

    fun testTotalKotlinOnlyLightTree() {
        totalKotlinLight(true)
    }

    fun testTotalKotlinOnlyPsi() {
        totalKotlinPsi(true)
    }

    fun testTotalKotlinFirFromLightTree() {
        totalKotlinLight(false)
    }

    fun testTotalKotlinFirFromPsi() {
        totalKotlinPsi(false)
    }
}
