/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import java.io.File
import kotlin.system.measureNanoTime

@TestDataPath("\$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners::class)
class RawFirBuilderTotalKotlinTestCase : AbstractRawFirBuilderTestCase() {

    fun testTotalKotlin() {
        val root = File(testDataPath)
        var counter = 0
        var time = 0L
        var totalLength = 0
        println("BASE PATH: $testDataPath")
        for (file in root.walkTopDown()) {
            if (file.isDirectory) continue
            if (file.path.contains("testData") || file.path.contains("resources")) continue
            if (file.extension != "kt") continue
            try {
                val ktFile = createKtFile(file.toRelativeString(root))
                var firFile: FirFile? = null
                time += measureNanoTime {
                    firFile = ktFile.toFirFile()
                }
                totalLength += StringBuilder().also { FirRenderer(it).visitFile(firFile!!) }.length
                counter++
            } catch (e: Exception) {
                println("TIME PER FILE: ${(time / counter) * 1e-6} ms, COUNTER: $counter")
                println("EXCEPTION in: " + file.toRelativeString(root))
                throw e
            }
        }
        println("SUCCESS!")
        println("TOTAL LENGTH: $totalLength")
        println("TIME PER FILE: ${(time / counter) * 1e-6} ms, COUNTER: $counter")
    }

}