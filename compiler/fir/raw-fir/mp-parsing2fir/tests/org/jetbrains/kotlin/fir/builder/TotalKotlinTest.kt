/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.impl.DebugUtil
import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.session.FirSessionFactoryHelper
import org.jetbrains.kotlin.parsing.KotlinLightParser
import org.jetbrains.kotlin.test.util.walkRepositoryKotlinFilesWithoutTestData
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.system.measureNanoTime

@TestDataPath("/")
class TotalKotlinTest : AbstractRawFirBuilderTestCase() {
    private fun generateFirFromLightTree(
        onlyLightTree: Boolean, converter: MultiplatformParsing2Fir,
        text: CharSequence, sourceFile: KtSourceFile, linesMapping: KtSourceFileLinesMapping
    ) {
        if (onlyLightTree) {
            val lightTree = KotlinLightParser.buildLightTree(text, sourceFile, errorListener = null)
            DebugUtil.lightTreeToString(lightTree, false)
        } else {
            val firFile = converter.buildFirFile(text, sourceFile, linesMapping)
            FirRenderer().renderElementAsString(firFile)
        }
    }

    private fun totalKotlinLight(onlyLightTree: Boolean) {
        // Back from /compiler/fir/raw-fir/<module>
        val path = System.getProperty("user.dir") + "/../../../.."
        var counter = 0
        var time = 0L

        @OptIn(ObsoleteTestInfrastructure::class)
        val lightTreeConverter = MultiplatformParsing2Fir(
            session = FirSessionFactoryHelper.createEmptySession(),
            scopeProvider = StubFirScopeProvider,
            diagnosticsReporter = null
        )

        if (onlyLightTree) println("LightTree generation") else println("Fir from LightTree converter")
        println("BASE PATH: ${File(path).normalize().absolutePath}")
        path.walkRepositoryKotlinFilesWithoutTestData {
            val sourceFile = KtIoFileSourceFile(it)
            val [code, linesMapping] = it.inputStream().reader(Charsets.UTF_8).use {
                it.readSourceFileWithMapping()
            }
            time += measureNanoTime {
                generateFirFromLightTree(onlyLightTree, lightTreeConverter, code, sourceFile, linesMapping)
            }

            counter++
        }
        println("SUCCESS!")
        println("TIME PER FILE: ${(time / counter) * 1e-6} ms, COUNTER: $counter")

    }

    @Test
    fun testTotalKotlinOnlyLightTree() {
        totalKotlinLight(true)
    }

    @Test
    fun testTotalKotlinFirFromLightTree() {
        totalKotlinLight(false)
    }
}
