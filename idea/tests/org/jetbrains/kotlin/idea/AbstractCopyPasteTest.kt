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

package org.jetbrains.kotlin.idea

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.JetFile
import java.io.File

public abstract class AbstractCopyPasteTest : JetLightCodeInsightFixtureTestCase() {
    private var savedImportsOnPasteSetting: Int = 0
    private val DEFAULT_TO_FILE_TEXT = "package to\n\n<caret>"

    override fun setUp() {
        super.setUp()
        savedImportsOnPasteSetting = CodeInsightSettings.getInstance().ADD_IMPORTS_ON_PASTE
        CodeInsightSettings.getInstance().ADD_IMPORTS_ON_PASTE = CodeInsightSettings.YES
    }

    override fun tearDown() {
        CodeInsightSettings.getInstance().ADD_IMPORTS_ON_PASTE = savedImportsOnPasteSetting
        super.tearDown()
    }

    protected fun configureByDependencyIfExists(dependencyFileName: String): PsiFile? {
        val file = File(getTestDataPath() + File.separator + dependencyFileName)
        if (!file.exists()) return null
        return if (dependencyFileName.endsWith(".java")) {
            //allow test framework to put it under right directory
            myFixture.addClass(FileUtil.loadFile(file, true)).getContainingFile()
        }
        else {
            myFixture.configureByFile(dependencyFileName)
        }
    }

    protected  fun configureTargetFile(fileName: String): JetFile {
        if (File(getTestDataPath() + File.separator + fileName).exists()) {
            return myFixture.configureByFile(fileName) as JetFile
        }
        else {
            return myFixture.configureByText(fileName, DEFAULT_TO_FILE_TEXT) as JetFile
        }
    }
}

