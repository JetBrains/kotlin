/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.checkers.javac

import org.jetbrains.kotlin.checkers.AbstractDiagnosticsTest
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import java.io.File

abstract class AbstractJavacDiagnosticsTest : AbstractDiagnosticsTest() {

    private var useJavac = true

    override fun analyzeAndCheck(testDataFile: File, files: List<TestFile>) {
        if (useJavac) {
            val groupedByModule = files.groupBy(TestFile::module)
            val allKtFiles = groupedByModule.values.flatMap { getKtFiles(it, true) }
            environment.registerJavac(kotlinFiles = allKtFiles)
            environment.configuration.put(JVMConfigurationKeys.USE_JAVAC, true)
        }
        super.analyzeAndCheck(testDataFile, files)
    }

    fun doTestWithoutJavacWrapper(path: String) {
        useJavac = false
        super.doTest(path)
    }

}

