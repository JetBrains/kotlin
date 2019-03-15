/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

abstract class AbstractCopyPasteTest : KotlinLightCodeInsightFixtureTestCase() {
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
        val file = File(testDataPath + File.separator + dependencyFileName)
        if (!file.exists()) return null
        return if (dependencyFileName.endsWith(".java")) {
            //allow test framework to put it under right directory
            myFixture.addClass(FileUtil.loadFile(file, true)).containingFile
        }
        else {
            myFixture.configureByFile(dependencyFileName)
        }
    }

    protected  fun configureTargetFile(fileName: String): KtFile {
        if (File(testDataPath + File.separator + fileName).exists()) {
            return myFixture.configureByFile(fileName) as KtFile
        }
        else {
            return myFixture.configureByText(fileName, DEFAULT_TO_FILE_TEXT) as KtFile
        }
    }
}

