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

package org.jetbrains.kotlin.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.impl.source.tree.FileElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.idea.JetFileType
import java.util.HashSet
import com.intellij.openapi.util.Key
import com.intellij.psi.impl.PsiModificationTrackerImpl
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.types.JetType
import java.util.LinkedHashSet

public abstract class JetCodeFragment(
        private val _project: Project,
        name: String,
        text: CharSequence,
        imports: String?, // Should be separated by JetCodeFragment.IMPORT_SEPARATOR
        elementType: IElementType,
        private val context: PsiElement?
): JetFile((PsiManager.getInstance(_project) as PsiManagerEx).getFileManager().createFileViewProvider(LightVirtualFile(name, JetFileType.INSTANCE, text), true), false), JavaCodeFragment {

    private var viewProvider = super<JetFile>.getViewProvider() as SingleRootFileViewProvider
    private var myImports = LinkedHashSet<String>();

    {
        getViewProvider().forceCachedPsi(this)
        init(TokenType.CODE_FRAGMENT, elementType)
        if (context != null) {
            initImports(context, imports)
        }
    }

    private var resolveScope: GlobalSearchScope? = null
    private var thisType: PsiType? = null
    private var superType: PsiType? = null
    private var exceptionHandler: JavaCodeFragment.ExceptionHandler? = null

    public abstract fun getContentElement(): JetElement?

    override fun forceResolveScope(scope: GlobalSearchScope?) {
        resolveScope = scope
    }

    override fun getForcedResolveScope() = resolveScope

    override fun isPhysical() = true

    override fun isValid() = true

    override fun getContext() = context

    override fun getResolveScope() = resolveScope ?: super<JetFile>.getResolveScope()

    override fun clone(): JetCodeFragment {
        val clone = cloneImpl(calcTreeElement().clone() as FileElement) as JetCodeFragment
        clone.setOriginalFile(this)
        clone.myImports = myImports
        clone.viewProvider = SingleRootFileViewProvider(PsiManager.getInstance(_project), LightVirtualFile(getName(), JetFileType.INSTANCE, getText()), true)
        clone.viewProvider.forceCachedPsi(clone)
        return clone
    }

    override fun getViewProvider() = viewProvider

    override fun getThisType() = thisType

    override fun setThisType(psiType: PsiType?) {
        thisType = psiType
    }

    override fun getSuperType() = superType

    override fun setSuperType(superType: PsiType?) {
        $superType = superType
    }

    override fun importsToString(): String {
        return myImports.join(IMPORT_SEPARATOR)
    }

    override fun addImportsFromString(imports: String?) {
        if (imports == null || imports.isEmpty()) return

        // We should increment modification tracker after inserting import in code fragment to invalidate resolve caches.
        // Without this modification references with new import won't be resolved without any modification in code fragment.
        // Also shorten references won't work.
        (PsiModificationTracker.SERVICE.getInstance(getProject()) as PsiModificationTrackerImpl).incOutOfCodeBlockModificationCounter()

        myImports.addAll(imports.split(IMPORT_SEPARATOR))
    }

    public fun importsAsImportList(): JetImportList? {
        return JetPsiFactory(this).createFile(myImports.join("\n")).getImportList()
    }

    override fun setVisibilityChecker(checker: JavaCodeFragment.VisibilityChecker?) { }

    override fun getVisibilityChecker() = JavaCodeFragment.VisibilityChecker.EVERYTHING_VISIBLE

    override fun setExceptionHandler(checker: JavaCodeFragment.ExceptionHandler?) {
        exceptionHandler = checker
    }

    override fun getExceptionHandler() = exceptionHandler

    override fun importClass(aClass: PsiClass?): Boolean {
        return true
    }

    private fun initImports(context: PsiElement, imports: String?) {
        val containingFile = context.getContainingFile()
        if (containingFile !is JetFile) return

        val importListForContextElement = containingFile.getImportList()
        if (importListForContextElement != null) {
            myImports.addAll(importListForContextElement.getImports().map { it.getText() })
        }

        val packageName = containingFile.getPackageDirective()?.getFqName()?.asString()
        if (packageName != null && packageName.isNotEmpty()) {
            myImports.add("import $packageName.*")
        }

        if (imports != null && !imports.isEmpty()) {
            myImports.addAll(imports.split(IMPORT_SEPARATOR))
        }
    }

    class object {
        public val IMPORT_SEPARATOR: String = ","
        public val RUNTIME_TYPE_EVALUATOR: Key<Function1<JetExpression, JetType?>> = Key.create("RUNTIME_TYPE_EVALUATOR")
    }
}
