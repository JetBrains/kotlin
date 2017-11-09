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

package org.jetbrains.kotlin.idea.structureView

import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.util.treeView.smartTree.*
import com.intellij.openapi.editor.Editor
import com.intellij.util.PlatformIcons
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile

class KotlinStructureViewModel(ktFile: KtFile, editor: Editor?) : StructureViewModelBase(ktFile, editor, KotlinStructureViewElement(ktFile, false)) {

    init {
        withSuitableClasses(KtDeclaration::class.java)
        withSorters(Sorter.ALPHA_SORTER)
    }

    override fun getNodeProviders() = NODE_PROVIDERS

    override fun getFilters() = FILTERS

    companion object {
        private val NODE_PROVIDERS = listOf(KotlinInheritedMembersNodeProvider())
        private val FILTERS = arrayOf<Filter>(PublicElementsFilter)
    }
}

object PublicElementsFilter : Filter {
    override fun isVisible(treeNode: TreeElement): Boolean {
        return (treeNode as? KotlinStructureViewElement)?.isPublic ?: true
    }

    override fun getPresentation(): ActionPresentation {
        return ActionPresentationData("Show non-public", null, PlatformIcons.PRIVATE_ICON)
    }

    override fun getName() = ID

    override fun isReverted() = true

    const val ID = "KOTLIN_SHOW_NON_PUBLIC"
}
