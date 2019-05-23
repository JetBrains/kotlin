/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.totalKotlin

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilderTestCase
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import java.io.File
import kotlin.system.measureNanoTime

@TestDataPath("/")
@RunWith(JUnit3RunnerWithInners::class)
abstract class AbstractTotalKotlinTest(private val treeName: String) : AbstractRawFirBuilderTestCase() {
    protected abstract fun generateTree(onlyBaseTree: Boolean, text: String, file: File)

    fun totalKotlinTest(onlyBaseTree: Boolean) {
        val path = System.getProperty("user.dir")
        val root = File(path)
        var counter = 0
        var time = 0L

        if (onlyBaseTree) println("$treeName generation") else println("Fir from $treeName converter")
        println("BASE PATH: $path")
        for (file in root.walkTopDown()) {
            if (file.isDirectory) continue
            if (file.path.contains("testData") || file.path.contains("resources")) continue
            if (file.extension != "kt") continue

            val text = FileUtil.loadFile(file, CharsetToolkit.UTF8, true).trim()
            time += measureNanoTime {
                generateTree(onlyBaseTree, text, file)
            }

            counter++
        }
        println("SUCCESS!")
        println("TIME PER FILE: ${(time / counter) * 1e-6} ms, COUNTER: $counter")
    }
}

