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

package org.jetbrains.kotlin.idea.js

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.ExternalLibrariesNode
import com.intellij.ide.projectView.impl.nodes.NamedLibraryElementNode
import com.intellij.ide.util.treeView.AbstractTreeNode

public class KotlinJavaScriptLibraryContentsTreeStructureProvider : TreeStructureProvider {

    override fun modify(parent: AbstractTreeNode<*>, children: Collection<AbstractTreeNode<*>>, settings: ViewSettings): Collection<AbstractTreeNode<*>> =
        if (parent.getProject() == null || parent !is ExternalLibrariesNode) children else filterLibraryNodes(children)

    private fun filterLibraryNodes(children: Collection<AbstractTreeNode<*>>): Collection<AbstractTreeNode<*>> {
        val filteredChildren = children.filterNot { it is NamedLibraryElementNode && KotlinJavaScriptLibraryManager.LIBRARY_NAME == it.getName() }
        return if (filteredChildren.size() == children.size()) children else filteredChildren
    }

    override fun getData(selected: MutableCollection<AbstractTreeNode<*>>?, dataName: String?): Any? = null
}
