/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.impl

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.test.services.TemporaryDirectoryManager
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.testInfo
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File
import java.util.*

class TemporaryDirectoryManagerImpl(testServices: TestServices) : TemporaryDirectoryManager(testServices) {
    private val cache = mutableMapOf<String, File>()
    private val rootTempDir: File = run {
        val testInfo = testServices.testInfo
        val className = testInfo.className
        val methodName = testInfo.methodName
        if (!onWindows && className.length + methodName.length < 255) {
            return@run KtTestUtil.tmpDirForTest(className, methodName)
        }

        // This code will simplify directory name for windows. This is needed because there can occur errors due to long name
        val lastDot = className.lastIndexOf('.')
        val packageName = className.substring(0, lastDot + 1)
        val simplifiedClassName = className.substring(lastDot + 1).getOnlyUpperCaseSymbols()
        val simplifiedMethodName = methodName.getOnlyUpperCaseSymbols()
        KtTestUtil.tmpDirForTest(packageName + simplifiedClassName, "test$simplifiedMethodName")
    }

    override val rootDir: File
        get() = rootTempDir

    override fun getOrCreateTempDirectory(name: String): File {
        return cache.getOrPut(name) { KtTestUtil.tmpDir(rootTempDir, name) }
    }

    override fun cleanupTemporaryDirectories() {
        cache.clear()
        FileUtil.delete(rootTempDir)
    }

    companion object {
        private val onWindows: Boolean = System.getProperty("os.name").lowercase(Locale.getDefault()).contains("windows")

        private fun String.getOnlyUpperCaseSymbols(): String {
            return this.filter { it.isUpperCase() || it == '$' }.toList().joinToString(separator = "")
        }
    }
}
