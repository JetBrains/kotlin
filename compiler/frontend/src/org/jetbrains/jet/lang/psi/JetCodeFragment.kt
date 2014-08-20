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
import java.util.HashSet

public abstract class JetCodeFragment(
        private val _project: Project,
        name: String,
        text: CharSequence,
        elementType: IElementType,
        private val _context: PsiElement?
): JetFile((PsiManager.getInstance(_project) as PsiManagerEx).getFileManager().createFileViewProvider(LightVirtualFile(name, JetFileType.INSTANCE, text), true), false), JavaCodeFragment {

    private var _viewProvider = super<JetFile>.getViewProvider() as SingleRootFileViewProvider
    private var _myImports = HashSet<String>();

    {
        getViewProvider().forceCachedPsi(this)
        init(TokenType.CODE_FRAGMENT, elementType)
        if (_context != null) {
            addImportsFromString(getImportsForElement(_context))
        }
    }

    private var _resolveScope: GlobalSearchScope? = null
    private var _thisType: PsiType? = null
    private var _superType: PsiType? = null
    private var _exceptionHandler: JavaCodeFragment.ExceptionHandler? = null

    public abstract fun getContentElement(): JetElement?

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
        clone._myImports = _myImports
        clone._viewProvider = SingleRootFileViewProvider(PsiManager.getInstance(_project), LightVirtualFile(getName(), JetFileType.INSTANCE, getText()), true)
        clone._viewProvider.forceCachedPsi(clone)
        return clone
    }

    override fun getViewProvider() = _viewProvider

    override fun getThisType() = _thisType

    override fun setThisType(psiType: PsiType?) {
        _thisType = psiType
    }

    override fun getSuperType() = _superType

    override fun setSuperType(superType: PsiType?) {
        _superType = superType
    }

    override fun importsToString(): String {
        return _myImports.makeString(IMPORT_SEPARATOR)
    }

    override fun addImportsFromString(imports: String?) {
        if (imports == null || imports.isEmpty()) return

        _myImports.addAll(imports.split(IMPORT_SEPARATOR))
    }

    public fun importsAsImportList(): JetImportList? {
        return JetPsiFactory(this).createFile(_myImports.makeString("\n")).getImportList()
    }

    override fun setVisibilityChecker(checker: JavaCodeFragment.VisibilityChecker?) { }

    override fun getVisibilityChecker() = JavaCodeFragment.VisibilityChecker.EVERYTHING_VISIBLE

    override fun setExceptionHandler(checker: JavaCodeFragment.ExceptionHandler?) {
        _exceptionHandler = checker
    }

    override fun getExceptionHandler() = _exceptionHandler

    override fun importClass(aClass: PsiClass?): Boolean {
        return true
    }

    class object {
        public val IMPORT_SEPARATOR: String = ","

        public fun getImportsForElement(elementAtCaret: PsiElement): String {
            val containingFile = elementAtCaret.getContainingFile()
            if (containingFile !is JetFile) return ""

            return containingFile.getImportList()?.getImports()
                        ?.map { it.getText() }
                        ?.makeString(JetCodeFragment.IMPORT_SEPARATOR) ?: ""
        }
    }
}
