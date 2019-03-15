/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.apache.commons.io.FileUtils
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import java.io.File

abstract class AbstractKotlinSourceInJavaCompletionTest : KotlinFixtureCompletionBaseTestCase() {
    override fun getPlatform() = JvmPlatform

    override fun doTest(testPath: String) {
        val mockPath = RELATIVE_COMPLETION_TEST_DATA_BASE_PATH + "/injava/mockLib"
        val mockLibDir = File(mockPath)
        fun collectPaths(dir: File): List<String> {
            return dir.listFiles()!!.flatMap {
                if (it.isDirectory) {
                    collectPaths(it)
                } else listOf(FileUtil.toSystemIndependentName(it.path))
            }
        }

        val paths = collectPaths(mockLibDir).toTypedArray()
        paths.forEach { path ->
            val vFile = myFixture.copyFileToProject(path, path.substring(mockPath.length))
            myFixture.configureFromExistingVirtualFile(vFile)
        }

        LightClassComputationControl.testWithControl(project, FileUtil.loadFile(File(testPath))) {
            super.doTest(testPath)
        }
    }

    override fun getProjectDescriptor() = LightCodeInsightFixtureTestCase.JAVA_LATEST
    override fun defaultCompletionType() = CompletionType.BASIC
}