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