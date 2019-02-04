/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.coverage

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractKotlinCoverageOutputFilesTest(): KotlinLightCodeInsightFixtureTestCase() {
    private val TEST_DATA_PATH = PluginTestCaseBase.getTestDataPathBase() + "/coverage/outputFiles"

    override fun getTestDataPath(): String = TEST_DATA_PATH

    fun doTest(path: String) {
        val kotlinFile = myFixture.configureByFile(path) as KtFile
        val outDir = myFixture.tempDirFixture.findOrCreateDir("coverageTestOut")
        try {
            FileUtil.loadLines(File(path.replace(".kt", ".classes.txt"))).forEach {
                runWriteAction {
                    createEmptyFile(outDir, it)
                }
            }

            val actualClasses = KotlinCoverageExtension.collectGeneratedClassQualifiedNames(outDir, kotlinFile)
            KotlinTestUtils.assertEqualsToFile(File(path.replace(".kt", ".expected.txt")), actualClasses!!.joinToString("\n"))
        }
        finally {
            runWriteAction {
                outDir.delete(null)
            }
        }
    }
}

private fun createEmptyFile(dir: VirtualFile, relativePath: String) {
    var currentDir = dir
    val segments = relativePath.split('/')
    segments.forEachIndexed { i, s ->
        if (i < segments.size - 1) {
            currentDir = currentDir.createChildDirectory(null, s)
        } else {
            currentDir.createChildData(null, s)
        }
    }
}
