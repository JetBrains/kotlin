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

package org.jetbrains.jet.lang.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.impl.source.tree.FileElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.jet.plugin.JetFileType

public abstract class JetCodeFragment(
        val _project: Project,
        name: String,
        text: CharSequence,
        elementType: IElementType,
        val _context: PsiElement?
): JetFile((PsiManager.getInstance(_project) as PsiManagerEx).getFileManager().createFileViewProvider(LightVirtualFile(name, JetFileType.INSTANCE, text), true), false), PsiCodeFragment {

    {
        (getViewProvider() as SingleRootFileViewProvider).forceCachedPsi(this)
        init(TokenType.CODE_FRAGMENT, elementType)
    }

    private var _resolveScope: GlobalSearchScope? = null
    private var _viewProvider: FileViewProvider? = null

    override fun forceResolveScope(scope: GlobalSearchScope?) {
        _resolveScope = scope
    }

    override fun getForcedResolveScope() = _resolveScope

    override fun isPhysical() = true

    override fun isValid() = true

    override fun getContext() = _context

    override fun getResolveScope() = _resolveScope ?: super<JetFile>.getResolveScope()

    override fun clone(): JetCodeFragment {
        val clone = cloneImpl(calcTreeElement().clone() as FileElement) as JetCodeFragment
        clone.setOriginalFile(this)
        val fileManager = (PsiManager.getInstance(_project) as PsiManagerEx).getFileManager()
        val cloneViewProvider = fileManager.createFileViewProvider(LightVirtualFile(getName(), JetFileType.INSTANCE, getText()), true) as SingleRootFileViewProvider
        cloneViewProvider.forceCachedPsi(clone)
        clone._viewProvider = cloneViewProvider
        return clone
    }

    override fun getViewProvider() = _viewProvider ?: super<JetFile>.getViewProvider()
}
