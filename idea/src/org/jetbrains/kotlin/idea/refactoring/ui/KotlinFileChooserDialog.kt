/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.ui

import com.intellij.ide.util.AbstractTreeClassChooserDialog
import com.intellij.ide.util.gotoByName.GotoFileModel
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.projectView.KtClassOrObjectTreeNode
import org.jetbrains.kotlin.idea.projectView.KtFileTreeNode
import org.jetbrains.kotlin.idea.search.projectScope
import org.jetbrains.kotlin.idea.search.restrictToKotlinSources
import org.jetbrains.kotlin.psi.KtFile
import javax.swing.tree.DefaultMutableTreeNode

class KotlinFileChooserDialog(
        title: String,
        project: Project
) : AbstractTreeClassChooserDialog<KtFile>(
        title,
        project,
        project.projectScope().restrictToKotlinSources(),
        KtFile::class.java,
        null,
        null,
        null,
        false,
        false
) {
    override fun getSelectedFromTreeUserObject(node: DefaultMutableTreeNode): KtFile? {
        val userObject = node.userObject
        return when (userObject) {
            is KtFileTreeNode -> userObject.ktFile
            is KtClassOrObjectTreeNode -> {
                val containingFile = userObject.value.getContainingKtFile()
                if (containingFile.declarations.size == 1) containingFile else null
            }
            else -> null
        }
    }

    override fun getClassesByName(name: String, checkBoxState: Boolean, pattern: String, searchScope: GlobalSearchScope): List<KtFile> {
        return FilenameIndex.getFilesByName(project, name, searchScope).filterIsInstance<KtFile>()
    }

    override fun createChooseByNameModel() = GotoFileModel(this.project)
}