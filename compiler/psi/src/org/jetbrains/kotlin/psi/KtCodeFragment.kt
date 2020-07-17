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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.JavaCodeFragment.VisibilityChecker
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.impl.source.tree.FileElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

abstract class KtCodeFragment(
    private val myProject: Project,
    name: String,
    text: CharSequence,
    imports: String?, // Should be separated by KtCodeFragment.IMPORT_SEPARATOR
    elementType: IElementType,
    private val context: PsiElement?
) : KtFile(
    run {
        val psiManager = PsiManager.getInstance(myProject) as PsiManagerEx
        psiManager.fileManager.createFileViewProvider(LightVirtualFile(name, KotlinFileType.INSTANCE, text), true)
    }, false
), KtCodeFragmentBase {
    private var viewProvider = super.getViewProvider() as SingleRootFileViewProvider
    private var imports = LinkedHashSet<String>()

    private val fakeContextForJavaFile: PsiElement? by lazy {
        this.getCopyableUserData(FAKE_CONTEXT_FOR_JAVA_FILE)?.invoke()
    }

    init {
        @Suppress("LeakingThis")
        getViewProvider().forceCachedPsi(this)
        init(TokenType.CODE_FRAGMENT, elementType)
        if (context != null) {
            initImports(imports)
        }
    }

    final override fun init(elementType: IElementType, contentElementType: IElementType?) {
        super.init(elementType, contentElementType)
    }

    private var resolveScope: GlobalSearchScope? = null
    private var thisType: PsiType? = null
    private var superType: PsiType? = null
    private var exceptionHandler: JavaCodeFragment.ExceptionHandler? = null
    private var isPhysical = true

    abstract fun getContentElement(): KtElement?

    override fun forceResolveScope(scope: GlobalSearchScope?) {
        resolveScope = scope
    }

    override fun getForcedResolveScope() = resolveScope

    override fun isPhysical() = isPhysical

    override fun isValid() = true

    override fun getContext(): PsiElement? {
        if (fakeContextForJavaFile != null) return fakeContextForJavaFile
        if (context !is KtElement) {
            val logInfoForContextElement = (context as? PsiFile)?.virtualFile?.path ?: context?.getElementTextWithContext()
            LOG.warn("CodeFragment with non-kotlin context should have fakeContextForJavaFile set: \noriginalContext = $logInfoForContextElement")
            return null
        }

        return context
    }

    override fun getResolveScope() = context?.resolveScope ?: super.getResolveScope()

    override fun clone(): KtCodeFragment {
        val elementClone = calcTreeElement().clone() as FileElement

        return (cloneImpl(elementClone) as KtCodeFragment).apply {
            isPhysical = false
            myOriginalFile = this@KtCodeFragment
            imports = this@KtCodeFragment.imports
            viewProvider = SingleRootFileViewProvider(
                PsiManager.getInstance(myProject),
                LightVirtualFile(name, KotlinFileType.INSTANCE, text),
                false
            )
            viewProvider.forceCachedPsi(this)
        }
    }

    final override fun getViewProvider() = viewProvider

    override fun getThisType() = thisType

    override fun setThisType(psiType: PsiType?) {
        thisType = psiType
    }

    override fun getSuperType() = superType

    override fun setSuperType(superType: PsiType?) {
        this.superType = superType
    }

    override fun importsToString(): String {
        return imports.joinToString(IMPORT_SEPARATOR)
    }

    override fun addImportsFromString(imports: String?) {
        if (imports == null || imports.isEmpty()) return

        imports.split(IMPORT_SEPARATOR).forEach {
            addImport(it)
        }

        // we need this code to force re-highlighting, otherwise it does not work by some reason
        val tempElement = KtPsiFactory(project).createColon()
        add(tempElement).delete()
    }

    fun addImport(import: String) {
        val contextFile = getContextContainingFile()
        if (contextFile != null) {
            if (contextFile.importDirectives.find { it.text == import } == null) {
                imports.add(import)
            }
        }
    }

    fun importsAsImportList(): KtImportList? {
        if (imports.isNotEmpty() && context != null) {
            return KtPsiFactory(this).createAnalyzableFile("imports_for_codeFragment.kt", imports.joinToString("\n"), context).importList
        }
        return null
    }

    override val importDirectives: List<KtImportDirective>
        get() = importsAsImportList()?.imports ?: emptyList()

    override fun setVisibilityChecker(checker: VisibilityChecker?) {}

    override fun getVisibilityChecker(): VisibilityChecker = VisibilityChecker.EVERYTHING_VISIBLE

    override fun setExceptionHandler(checker: JavaCodeFragment.ExceptionHandler?) {
        exceptionHandler = checker
    }

    override fun getExceptionHandler() = exceptionHandler

    fun getContextContainingFile(): KtFile? {
        return getOriginalContext()?.takeIf { it.isValid }?.containingKtFile
    }

    fun getOriginalContext(): KtElement? {
        val contextElement = getContext() as? KtElement
        val contextFile = contextElement?.containingFile as? KtFile
        if (contextFile is KtCodeFragment) {
            return contextFile.getOriginalContext()
        }
        return contextElement
    }

    private fun initImports(imports: String?) {
        if (imports != null && imports.isNotEmpty()) {
            val importsWithPrefix = imports.split(IMPORT_SEPARATOR).map { it.takeIf { it.startsWith("import ") } ?: "import ${it.trim()}" }
            importsWithPrefix.forEach {
                addImport(it)
            }
        }
    }

    companion object {
        const val IMPORT_SEPARATOR: String = ","
        val RUNTIME_TYPE_EVALUATOR: Key<Function1<KtExpression, KotlinType?>> = Key.create("RUNTIME_TYPE_EVALUATOR")
        val FAKE_CONTEXT_FOR_JAVA_FILE: Key<Function0<KtElement>> = Key.create("FAKE_CONTEXT_FOR_JAVA_FILE")

        private val LOG = Logger.getInstance(KtCodeFragment::class.java)
    }
}

var KtCodeFragment.externalDescriptors: List<DeclarationDescriptor>? by CopyablePsiUserDataProperty(Key.create("EXTERNAL_DESCRIPTORS"))