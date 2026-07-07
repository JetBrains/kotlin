/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.mock.MockProject
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
import com.intellij.util.messages.Topic
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtCodeFragment.Companion.IMPORT_SEPARATOR
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext

/**
 * A synthetic, in-memory Kotlin file that holds a standalone snippet of code together with a context.
 *
 * Code fragments are not part of any real source file. They are created for scenarios such as evaluating a debugger
 * watch expression, previewing a refactoring, or running an intention, where a piece of Kotlin code must be parsed and
 * analyzed as if it appeared at a particular [context] element. The context determines which declarations are visible
 * to the fragment, and additional [imports] can be supplied on top of it.
 *
 * This is the common base for the concrete node types [KtExpressionCodeFragment], [KtBlockCodeFragment], and
 * [KtTypeCodeFragment]; the meaningful content is exposed by [getContentElement].
 *
 * @param imports optional imports, separated by [IMPORT_SEPARATOR]
 * @param context the element the fragment is conceptually placed at, used to resolve references from the fragment
 */
abstract class KtCodeFragment(
    viewProvider: FileViewProvider,
    imports: String?, // Should be separated by KtCodeFragment.IMPORT_SEPARATOR
    elementType: IElementType,
    private val context: PsiElement?
) : KtFile(viewProvider, false), KtCodeFragmentBase {
    constructor(
        project: Project,
        name: String,
        text: CharSequence,
        imports: String?,
        elementType: IElementType,
        context: PsiElement?
    ) : this(
        createFileViewProviderForLightFile(project, name, text),
        imports,
        elementType,
        context,
    )

    private var viewProvider = super.getViewProvider() as SingleRootFileViewProvider
    private var importDirectiveStrings = LinkedHashSet<String>()

    private val fakeContextForJavaFile: PsiElement? by lazy {
        this.getCopyableUserData(FAKE_CONTEXT_FOR_JAVA_FILE)?.invoke()
    }

    init {
        @Suppress("LeakingThis")
        getViewProvider().forceCachedPsi(this)
        init(TokenType.CODE_FRAGMENT, elementType)

        if (imports != null) {
            appendImports(imports)
        }
    }

    final override fun init(elementType: IElementType, contentElementType: IElementType?) {
        super.init(elementType, contentElementType)
    }

    private var resolveScope: GlobalSearchScope? = null
    private var thisType: PsiType? = null
    private var superType: PsiType? = null
    private var exceptionHandler: JavaCodeFragment.ExceptionHandler? = null

    /**
     * Returns the meaningful content of this fragment (for example, the parsed expression, block, or type reference),
     * or `null` if it could not be parsed.
     */
    abstract fun getContentElement(): KtElement?

    override fun forceResolveScope(scope: GlobalSearchScope?) {
        resolveScope = scope
    }

    override fun getForcedResolveScope() = resolveScope

    override fun isPhysical() = viewProvider.isEventSystemEnabled

    /** Always `true`: a code fragment is synthetic and always considered valid. */
    override fun isValid() = true

    override fun getContext(): PsiElement? {
        if (fakeContextForJavaFile != null) return fakeContextForJavaFile
        if (context != null && context !is KtElement) {
            val logInfoForContextElement = (context as? PsiFile)?.virtualFile?.path ?: context.getElementTextWithContext()
            LOG.warn("CodeFragment with non-kotlin context should have fakeContextForJavaFile set: \noriginalContext = $logInfoForContextElement")
            return null
        }

        return context
    }

    override fun getResolveScope() = context?.resolveScope ?: super.getResolveScope()

    override fun clone(): KtCodeFragment {
        val elementClone = calcTreeElement().clone() as FileElement

        return (cloneImpl(elementClone) as KtCodeFragment).apply {
            myOriginalFile = this@KtCodeFragment
            importDirectiveStrings = LinkedHashSet(this@KtCodeFragment.importDirectiveStrings)
            viewProvider = SingleRootFileViewProvider(
                PsiManager.getInstance(project),
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
        return importDirectiveStrings.joinToString(IMPORT_SEPARATOR)
    }

    override fun addImportsFromString(imports: String?) {
        val notifyChanged = viewProvider.isEventSystemEnabled && project !is MockProject

        if (imports != null && appendImports(imports)) {
            if (notifyChanged) {
                // This forces the code fragment to be re-highlighted
                add(KtPsiFactory(project).createColon()).delete()
            }

            // Increment the modification stamp
            clearCaches()

            if (notifyChanged) {
                @OptIn(KtImplementationDetail::class)
                project.messageBus.syncPublisher(IMPORT_MODIFICATION).onCodeFragmentImportsModification(this)
            }
        }
    }

    @Deprecated("Use 'addImportsFromString()w' instead", ReplaceWith("addImportsFromString(import)"), level = DeprecationLevel.WARNING)
    fun addImport(import: String) {
        addImportsFromString(import)
    }

    /**
     * Parses raw [rawImports] and appends them to the list of code fragment imports.
     *
     * Import strings must be separated by the [IMPORT_SEPARATOR].
     * Each import must be either a qualified name to import (e.g., 'foo.bar'), or a complete text representation of an import directive
     * (e.g., 'import foo.bar as baz').
     *
     * Note that already present import directives will be ignored.
     *
     * @return `true` if new import directives were added.
     */
    private fun appendImports(rawImports: String): Boolean {
        if (rawImports.isEmpty()) {
            return false
        }

        var hasNewImports = false

        for (rawImport in rawImports.split(IMPORT_SEPARATOR)) {
            val importDirectiveString = if (rawImport.startsWith("import ")) rawImport else "import $rawImport"
            if (importDirectiveStrings.add(importDirectiveString) && !hasNewImports) {
                hasNewImports = true
            }
        }

        return hasNewImports
    }

    /**
     * Builds a [KtImportList] from the imports registered on this fragment, or `null` if the fragment has no imports or
     * no context.
     */
    fun importsAsImportList(): KtImportList? {
        if (importDirectiveStrings.isNotEmpty() && context != null) {
            val ktPsiFactory = KtPsiFactory.contextual(context)
            val fileText = importDirectiveStrings.joinToString("\n")
            return ktPsiFactory.createFile("imports_for_codeFragment.kt", fileText).importList
        }

        return null
    }

    override val importList: KtImportList?
        get() = importsAsImportList()

    override val importLists: List<KtImportList>
        get() = listOfNotNull(importsAsImportList())

    override val importDirectives: List<KtImportDirective>
        get() = importsAsImportList()?.imports ?: emptyList()

    override fun setVisibilityChecker(checker: VisibilityChecker?) {}

    override fun getVisibilityChecker(): VisibilityChecker = VisibilityChecker.EVERYTHING_VISIBLE

    override fun setExceptionHandler(checker: JavaCodeFragment.ExceptionHandler?) {
        exceptionHandler = checker
    }

    override fun getExceptionHandler() = exceptionHandler

    /**
     * Returns the [KtFile] that ultimately contains this fragment's context (see [getOriginalContext]), or `null` if
     * the context is invalid or not a Kotlin element.
     */
    fun getContextContainingFile(): KtFile? {
        return getOriginalContext()?.takeIf { it.isValid }?.containingKtFile
    }

    /**
     * Returns the original context element, unwrapping nested code fragments so that the result is a real source
     * element rather than another fragment's context, or `null` if there is no Kotlin context.
     */
    fun getOriginalContext(): KtElement? {
        val contextElement = getContext() as? KtElement
        val contextFile = contextElement?.containingFile as? KtFile
        if (contextFile is KtCodeFragment) {
            return contextFile.getOriginalContext()
        }
        return contextElement
    }

    companion object {
        /**
         * The separator between individual imports in the raw import string accepted by a code fragment.
         */
        const val IMPORT_SEPARATOR: String = ","

        @KtImplementationDetail
        val IMPORT_MODIFICATION: Topic<KotlinCodeFragmentImportModificationListener> =
            Topic(KotlinCodeFragmentImportModificationListener::class.java, Topic.BroadcastDirection.TO_CHILDREN, true)

        /**
         * A copyable user-data key holding a provider of a fake Kotlin context element. It is used when a fragment's
         * real context is a non-Kotlin (for example, Java) file, so that references can still be resolved as Kotlin.
         */
        val FAKE_CONTEXT_FOR_JAVA_FILE: Key<Function0<KtElement>> = Key.create("FAKE_CONTEXT_FOR_JAVA_FILE")

        internal fun createFileViewProviderForLightFile(project: Project, name: String, text: CharSequence): FileViewProvider {
            val psiManager = PsiManager.getInstance(project) as PsiManagerEx
            return psiManager.fileManager.createFileViewProvider(
                LightVirtualFile(name, KotlinFileType.INSTANCE, text),
                /* eventSystemEnabled = */true
            )
        }

        private val LOG = Logger.getInstance(KtCodeFragment::class.java)
    }
}

/**
 * A listener notified when the imports of a [KtCodeFragment] are modified.
 */
fun interface KotlinCodeFragmentImportModificationListener {
    /**
     * Called after the imports of [codeFragment] have changed.
     */
    fun onCodeFragmentImportsModification(codeFragment: KtCodeFragment)
}