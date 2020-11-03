/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.trackers

import com.intellij.openapi.components.service
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiDocumentManager
import junit.framework.Assert
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

abstract class AbstractProjectWideOutOfBlockKotlinModificationTrackerTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun isFirPlugin(): Boolean = true

    fun doTest(path: String) {
        val testDataFile = File(path)
        val fileText = FileUtil.loadFile(testDataFile)
        myFixture.configureByText(testDataFile.name, fileText)
        val textToType = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// TYPE:") ?: DEFAULT_TEXT_TO_TYPE
        val outOfBlock = InTextDirectivesUtils.getPrefixedBoolean(fileText, "// OUT_OF_BLOCK:")
            ?: error("Please, specify should out of block change happen or not by `// OUT_OF_BLOCK:` directive")
        val tracker = project.service<KotlinFirOutOfBlockModificationTrackerFactory>().createProjectWideOutOfBlockModificationTracker()
        val initialModificationCount = tracker.modificationCount
        myFixture.type(textToType)
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val afterTypingModificationCount = tracker.modificationCount
        Assert.assertEquals(outOfBlock, initialModificationCount != afterTypingModificationCount)
    }

    companion object {
        private const val DEFAULT_TEXT_TO_TYPE = "hello"
    }
}