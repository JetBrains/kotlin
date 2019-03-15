/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions

import com.intellij.codeInsight.navigation.GotoTargetHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiFile
import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.kotlin.idea.navigation.NavigationTestUtils
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractNavigationTest : KotlinLightCodeInsightFixtureTestCase() {
    protected abstract fun getSourceAndTargetElements(editor: Editor, file: PsiFile): GotoTargetHandler.GotoData?

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    protected open fun configureExtra(mainFileBaseName: String, mainFileText: String) {

    }

    protected fun doTest(path: String) {
        val mainFile = File(path)
        val fileText = FileUtil.loadFile(mainFile, true)

        try {
            ConfigLibraryUtil.configureLibrariesByDirective(myModule, PlatformTestUtil.getCommunityPath(), fileText)

            myFixture.testDataPath = "${KotlinTestUtils.getHomeDirectory()}/${mainFile.parent}"

            val mainFileName = mainFile.name
            val mainFileBaseName = mainFileName.substring(0, mainFileName.indexOf('.'))
            configureExtra(mainFileBaseName, fileText)
            mainFile.parentFile
                    .listFiles { _, name ->
                        name != mainFileName && name.startsWith("$mainFileBaseName.") && (name.endsWith(".kt") || name.endsWith(".java") || name.endsWith(".xml"))
                    }
                    .forEach{ myFixture.configureByFile(it.name) }
            val file = myFixture.configureByFile(mainFileName)

            NavigationTestUtils.assertGotoDataMatching(editor, getSourceAndTargetElements(editor, file))
        }
        finally {
            ConfigLibraryUtil.unconfigureLibrariesByDirective(myModule, fileText)
        }
    }
}

