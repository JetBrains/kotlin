/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.kotlin.test.testFramework

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.DebugUtil
import com.intellij.testFramework.TestDataFile
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.create
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase.assertSameLinesWithFile
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.io.IOException

@Suppress("UnstableApiUsage")
abstract class KtParsingTestCase protected constructor(@NonNls dataPath: String, fileExt: String) {
    protected var myFileExt: String = fileExt
    protected val myFullDataPath: String = "$testDataPath/$dataPath"
    protected lateinit var myFile: PsiFile
    protected val myProject: MockProject get() = myEnvironment.project as MockProject
    protected val testRootDisposable: Disposable = Disposer.newDisposable()

    private lateinit var myEnvironment: KotlinCoreEnvironment

    @BeforeEach
    fun setUp() {
        val configuration = CompilerConfiguration.create()
        @OptIn(CoreEnvironmentDeprecation::class)
        myEnvironment = KotlinCoreEnvironment.createForParallelTests(
            testRootDisposable, configuration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
    }

    @AfterEach
    fun tearDown() {
        disposeRootDisposable(testRootDisposable)
    }

    protected val testDataPath: String
        get() = KtTestUtil.getHomeDirectory()

    protected fun createPsiFile(name: String?, text: String): PsiFile {
        return KtTestUtil.createFile(name + "." + myFileExt, text, myProject)
    }

    @Throws(IOException::class)
    protected fun loadFile(@NonNls @TestDataFile name: String): String {
        return FileUtil.loadFile(File(myFullDataPath, name), CharsetToolkit.UTF8, true).trim { it <= ' ' }
    }

    companion object {
        @Throws(IOException::class)
        fun doCheckResult(fullPath: String?, targetDataName: String, actual: String) {
            val expectedFileName = fullPath + File.separatorChar + targetDataName
            assertSameLinesWithFile(expectedFileName, actual)
        }

        @JvmStatic
        protected fun toParseTreeText(file: PsiElement, skipSpaces: Boolean, printRanges: Boolean): String {
            val showWhitespaces = !skipSpaces
            return DebugUtil.psiToString(file, showWhitespaces, printRanges)
        }
    }
}
