/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.structureView

import org.jetbrains.jet.plugin.PluginTestCaseBase
import org.jetbrains.jet.plugin.JetLightCodeInsightFixtureTestCase
import com.intellij.util.ui.tree.TreeUtil
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.ui.treeStructure.filtered.FilteringTreeStructure
import com.intellij.ide.util.FileStructurePopup
import com.intellij.ide.actions.ViewStructureAction
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import org.jetbrains.jet.JetTestUtils

public abstract class AbstractKotlinFileStructureTest : JetLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/structureView/fileStructure"

    public fun doTest(path: String) {
        myFixture.configureByFile(path)

        val textEditor = TextEditorProvider.getInstance()!!.getTextEditor(myFixture.getEditor())
        val popup = ViewStructureAction.createPopup(myFixture.getProject(), textEditor)

        if (popup == null) throw AssertionError("popup mustn't be null")

        popup.createCenterPanel()
        popup.getTreeBuilder().getUi()!!.getUpdater()!!.setPassThroughMode(true)
        popup.update()

        val popupText = PlatformTestUtil.print(popup.getTree(), false)
        JetTestUtils.assertEqualsToFile(File("${FileUtil.getNameWithoutExtension(path)}.after"), popupText)
    }

    public fun FileStructurePopup.update() {
        getTreeBuilder().refilter()!!.doWhenProcessed {
            getStructure().rebuild()
            updateRecursively(getRootNode())
            getTreeBuilder().updateFromRoot()

            TreeUtil.expandAll(getTree())
        }
    }

    fun FileStructurePopup.getFileStructureSpeedSearch() = getSpeedSearch() as FileStructurePopup.MyTreeSpeedSearch

    fun FileStructurePopup.getStructure() = getTreeBuilder().getTreeStructure() as FilteringTreeStructure

    fun FileStructurePopup.getRootNode() = getTreeBuilder().getRootElement() as FilteringTreeStructure.FilteringNode

    fun FileStructurePopup.updateRecursively(node: FilteringTreeStructure.FilteringNode) {
        node.update()
        for (child in node.children()!!) {
            updateRecursively(child)
        }
    }
}
