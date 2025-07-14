/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.impl

import com.intellij.openapi.util.io.NioFiles
import org.jetbrains.kotlin.test.services.TemporaryDirectoryManager
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.testInfo
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File
import java.nio.file.Paths
import java.util.*

class TemporaryDirectoryManagerImpl(testServices: TestServices) : TemporaryDirectoryManager(testServices) {
    private val cache = mutableMapOf<String, File>()
    private val rootTempDir = lazy {
        val testInfo = testServices.testInfo
        val className = testInfo.className
        val methodName = testInfo.methodName
        if (!onWindows && className.length + methodName.length < 255) {
            return@lazy KtTestUtil.tmpDirForTest(className, methodName)
        }

        // This code will simplify directory name for windows. This is needed because there can occur errors due to long name
        val lastDot = className.lastIndexOf('.')
        val simplifiedClassName = className.substring(lastDot + 1).getOnlyUpperCaseSymbols()
        val simplifiedMethodName = methodName.getOnlyUpperCaseSymbols()
        KtTestUtil.tmpDirForTest(simplifiedClassName, simplifiedMethodName)
    }

    override val rootDir: File
        get() = rootTempDir.value

    override fun getOrCreateTempDirectory(name: String): File {
        return cache.getOrPut(name) { KtTestUtil.tmpDir(rootDir, name) }
    }

    override fun cleanupTemporaryDirectories() {
        cache.clear()

        if (rootTempDir.isInitialized()) {
            NioFiles.deleteRecursively(Paths.get(rootDir.path))
        }
    }

    companion object {
        private val onWindows: Boolean = System.getProperty("os.name").lowercase(Locale.getDefault()).contains("windows")

        private fun String.getOnlyUpperCaseSymbols(): String {
            return this.filter { it.isUpperCase() || it == '$' }.toList().joinToString(separator = "")
        }
    }
}
