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

class TemporaryDirectoryManagerImpl(testServices: TestServices) : TemporaryDirectoryManager(testServices) {
    private val rootTempDir: File = run {
        val testInfo = testServices.testInfo
        KtTestUtil.tmpDirForTest(testInfo.className, testInfo.methodName)
    }

    override fun createTempDirectory(name: String): File {
        return KtTestUtil.tmpDir(rootTempDir, name)
    }

    override fun cleanupTemporaryDirectories() {
        FileUtil.delete(rootTempDir)
    }
}
