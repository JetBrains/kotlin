/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.android.tests

import com.google.common.base.StandardSystemProperty
import com.intellij.openapi.util.io.FileUtil
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class AndroidRunner {
    companion object {
        private lateinit var pathManager: PathManager

        @JvmStatic
        @AfterAll
        fun tearDown() {
            FileUtil.delete(File(pathManager.tmpFolder))
        }
    }

    @TestFactory
    fun runTests(): List<DynamicNode> {
        val tmpFolder = Files.createTempDirectory(
            Paths.get(StandardSystemProperty.JAVA_IO_TMPDIR.value()!!), null
        ).toFile()
        println("Created temporary folder for running android tests: ${tmpFolder.absolutePath}")
        pathManager = PathManager(tmpFolder.absolutePath)
        CodegenTestsOnAndroidGenerator.generate(pathManager)
        println("Run tests on Android...")
        return CodegenTestsOnAndroidRunner.runTestsInEmulator(pathManager)
    }
}
