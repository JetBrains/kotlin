/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.generate

import com.intellij.openapi.roots.ModuleRootManager
import org.jetbrains.kotlin.idea.actions.generate.KotlinGenerateTestSupportActionBase
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.InTextDirectivesUtils

abstract class AbstractGenerateTestSupportMethodActionTest : AbstractCodeInsightActionTest() {
    private fun setUpTestSourceRoot() {
        val module = myModule
        val model = ModuleRootManager.getInstance(module).modifiableModel
        val entry = model.contentEntries.single()
        val sourceFolderFile = entry.sourceFolderFiles.single()
        entry.removeSourceFolder(entry.sourceFolders.single())
        entry.addSourceFolder(sourceFolderFile, true)
        runWriteAction {
            model.commit()
            module.project.save()
        }
    }

    override fun createAction(fileText: String) =
            (super.createAction(fileText) as KotlinGenerateTestSupportActionBase).apply {
                testFrameworkToUse = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// TEST_FRAMEWORK:")
            }

    override fun doTest(path: String) {
        setUpTestSourceRoot()
        super.doTest(path)
    }
}