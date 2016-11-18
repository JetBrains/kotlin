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
import com.intellij.ide.util.treeView.smartTree.NodeProvider
import com.intellij.ide.util.treeView.smartTree.Sorter
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile

import java.util.Arrays

class KotlinStructureViewModel(jetFile: KtFile) : StructureViewModelBase(jetFile, KotlinStructureViewElement(jetFile, false)) {

    init {
        withSuitableClasses(KtDeclaration::class.java)
    }

    override fun getNodeProviders(): Collection<NodeProvider<*>> {
        return NODE_PROVIDERS
    }

    override fun getSorters(): Array<Sorter> {
        return Companion.sorters
    }

    companion object {
        private val NODE_PROVIDERS = Arrays.asList<NodeProvider<*>>(
                KotlinInheritedMembersNodeProvider())

        private val sorters = arrayOf(Sorter.ALPHA_SORTER)
    }
}
