/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.*
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ArrayFactory
import com.intellij.util.FileContentUtilCore
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.psi.stubs.KotlinFileStub
import org.jetbrains.kotlin.psi.stubs.KotlinFileStubKind
import org.jetbrains.kotlin.psi.stubs.KotlinImportDirectiveStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType

/**
 * This class represents kotlin psi file, independently of java psi (no [PsiClassOwner] super).
 * It can be created by [org.jetbrains.kotlin.parsing.KotlinCommonParserDefinition], if java psi is not available e.g., on JB Client.
 *
 * It's not supposed to be used directly, use [PsiFile] or if you need to check instanceof, check its' file type or language instead.
 */
@Deprecated("Don't use directly, use file.getFileType() instead")
open class KtCommonFile(viewProvider: FileViewProvider, val isCompiled: Boolean) :
    PsiFileBase(viewProvider, KotlinLanguage.INSTANCE),
    KtDeclarationContainer,
    KtAnnotated,
    KtElement,
    PsiNamedElement {

    @Volatile
    private var isScript: Boolean? = null

    @Volatile
    private var hasTopLevelCallables: Boolean? = null

    @Volatile
    private var pathCached: String? = null

    open val importList: KtImportList?
        get() = findChildByTypeOrClass(KtStubBasedElementTypes.IMPORT_LIST, KtImportList::class.java)

    @Volatile
    private var hasImportAlias: Boolean? = null

    fun hasImportAlias(): Boolean {
        val hasImportAlias = hasImportAlias
        if (hasImportAlias != null) return hasImportAlias

        val newValue = importLists.any(KtImportList::computeHasImportAlias)
        this.hasImportAlias = newValue
        return newValue
    }

    protected open val importLists: List<KtImportList>
        get() = findChildrenByTypeOrClass(KtStubBasedElementTypes.IMPORT_LIST, KtImportList::class.java).asList()

    val fileAnnotationList: KtFileAnnotationList?
        get() = findChildBeforeFirstDeclarationInclusiveByType(KtStubBasedElementTypes.FILE_ANNOTATION_LIST)

    open val importDirectives: List<KtImportDirective>
        get() = importLists.flatMap { it.imports }

    val packageDirective: KtPackageDirective?
        get() = findChildBeforeFirstDeclarationInclusiveByType(KtStubBasedElementTypes.PACKAGE_DIRECTIVE)

    var packageFqName: FqName
        get() = greenStub?.getPackageFqName() ?: packageDirective?.fqName ?: FqName.ROOT
        set(value) {
            val packageDirective = packageDirective
            if (packageDirective != null) {
                packageDirective.fqName = value
            } else {
                val newPackageDirective = KtPsiFactory(project).createPackageDirectiveIfNeeded(value) ?: return
                addAfter(newPackageDirective, null)
            }
        }

    @Deprecated("Use 'packageFqName' property instead", ReplaceWith("packageFqName"))
    val packageFqNameByTree: FqName
        get() = packageFqName

    val script: KtScript?
        get() {
            isScript?.let { if (!it) return null }
            greenStub?.let { if (!it.isScript()) return null }

            val result = findChildBeforeFirstDeclarationInclusiveByType<KtScript>(KtStubBasedElementTypes.SCRIPT)
            if (isScript == null) {
                isScript = result != null
            }

            return result
        }

    val virtualFilePath
        get(): String {
            pathCached?.let { return it }

            return virtualFile.path.also {
                pathCached = it
            }
        }

    @Deprecated("Use 'isScript()' instead", ReplaceWith("isScript()"))
    val isScriptByTree: Boolean
        get() = isScript()

    /**
     * @return modifier lists that do not belong to any declaration due to incomplete code or syntax errors
     */
    val danglingModifierLists: Array<out KtModifierList>
        get() = greenStub?.getChildrenByType(
            KtStubBasedElementTypes.MODIFIER_LIST,
            KtStubBasedElementTypes.MODIFIER_LIST.arrayFactory
        ) ?: findChildrenByClass(KtModifierList::class.java)

    /**
     * @return annotations that do not belong to any declaration due to incomplete code or syntax errors
     */
    val danglingAnnotations: List<KtAnnotationEntry>
        get() = danglingModifierLists.flatMap { obj: KtModifierList -> obj.annotationEntries }

    override fun getFileType(): FileType = KotlinFileType.INSTANCE

    override fun toString(): String = "KtFile: $name"

    /** A workaround to provide the proper stub builder for decompiled files until KT-78356 is fixed */
    @KtImplementationDetail
    open val customStubBuilder: StubBuilder? get() = null

    override fun getDeclarations(): List<KtDeclaration> {
        return greenStub?.getChildrenByType(KtFile.FILE_DECLARATION_TYPES, KtDeclaration.ARRAY_FACTORY)?.toList()
            ?: PsiTreeUtil.getChildrenOfTypeAsList(this, KtDeclaration::class.java)
    }

    /**
     * This is an optimized way to find a file child element in the header.
     *
     * Regular [findChildByTypeOrClass] will iterate through all childen that is especially quite expensive in the
     * case of [findChildByClass].
     * It will trigger psi calculation for all children even if the wanted element in the first child.
     *
     * So this function will iterate as a maximum only through all non-declarations in the beginning plus one declaration.
     * This one declaration processing is required to support the optimization for [KtScript] as well as it can be only in the beginning.
     */
    private fun <T : KtElementImplStub<out StubElement<T>>> findChildBeforeFirstDeclarationInclusiveByType(
        elementType: KtStubElementType<out StubElement<T>, T>,
    ): T? {
        val stub = greenStub
        if (stub != null) {
            for (stubElement in stub.childrenStubs) {
                val stubType = stubElement.stubType
                when (stubType) {
                    // Required element found
                    elementType -> {
                        @Suppress("UNCHECKED_CAST")
                        return stubElement.psi as T
                    }

                    // Elements from the header can be declared only before declarations
                    in KtFile.FILE_DECLARATION_TYPES -> return null
                    else -> {}
                }
            }

            // No element found, there is no sense to search further via ast
            return null
        }

        val children: Sequence<ASTNode> = node.children()
        for (child in children) {
            val childType = child.elementType
            when (childType) {
                // Required element found
                elementType -> {
                    @Suppress("UNCHECKED_CAST")
                    return child.psi as T
                }

                // Elements from the header can be declared only before declarations
                in KtFile.FILE_DECLARATION_TYPES -> return null
                else -> {}
            }
        }

        return null
    }

    fun <S : StubElement<P>, P : KtElementImplStub<S>> findChildByTypeOrClass(
        elementType: KtStubElementType<out S, P>,
        elementClass: Class<P>
    ): P? {
        val stub = greenStub
        if (stub != null) {
            val importListStub = stub.findChildStubByType(elementType)
            return importListStub?.psi
        }
        return findChildByClass(elementClass)
    }

    fun <T : KtElementImplStub<out StubElement<*>>> findChildrenByTypeOrClass(
        elementType: KtStubElementType<*, T>,
        elementClass: Class<T>
    ): Array<out T> {
        val stub = greenStub
        if (stub != null) {
            val arrayFactory: ArrayFactory<T> = elementType.arrayFactory
            return stub.getChildrenByType(elementType, arrayFactory)
        }
        return findChildrenByClass(elementClass)
    }


    fun findImportByAlias(name: String): KtImportDirective? {
        if (!hasImportAlias()) return null

        return importDirectives.firstOrNull { name == it.aliasName }
    }

    fun findAliasByFqName(fqName: FqName): KtImportAlias? {
        if (!hasImportAlias()) return null

        return importDirectives.firstOrNull {
            it.alias != null && fqName == it.importedFqName
        }?.alias
    }

    fun getNameForGivenImportAlias(name: Name): Name? {
        if (!hasImportAlias()) return null

        return importDirectives.find { it.importedName == name }?.importedFqName?.pathSegments()?.last()
    }

    override fun getStub(): KotlinFileStub? = super.getStub()?.let { it as KotlinFileStub }

    protected open val greenStub: KotlinFileStub?
        get() = super.getGreenStub()?.let { it as KotlinFileStub }

    override fun clearCaches() {
        @Suppress("RemoveExplicitSuperQualifier")
        super<PsiFileBase>.clearCaches()
        isScript = null
        hasTopLevelCallables = null
        pathCached = null
        hasImportAlias = null
    }

    fun isScript(): Boolean = isScript ?: greenStub?.isScript() ?: (script != null)

    fun hasTopLevelCallables(): Boolean {
        hasTopLevelCallables?.let { return it }

        val greenStub = greenStub
        val result = if (greenStub != null) {
            greenStub.kind is KotlinFileStubKind.WithPackage.Facade
        } else {
            node.children().any {
                val psi = it.psi
                (psi is KtProperty ||
                        psi is KtNamedFunction ||
                        psi is KtScript ||
                        psi is KtTypeAlias) && !psi.hasExpectModifier()
            }
        }

        hasTopLevelCallables = result
        return result
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is KtVisitor<*, *>) {
            @Suppress("UNCHECKED_CAST")
            accept(visitor as KtVisitor<Any, Any?>, null)
        } else {
            visitor.visitFile(this)
        }
    }

    override fun getContainingKtFile(): KtFile = this as KtFile

    override fun <D> acceptChildren(visitor: KtVisitor<Void, D>, data: D) {
        KtPsiUtil.visitChildren(this, visitor, data)
    }

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitKtCommonFile(this)
    }

    override fun getAnnotations(): List<KtAnnotation> =
        fileAnnotationList?.annotations ?: emptyList()

    override fun getAnnotationEntries(): List<KtAnnotationEntry> =
        fileAnnotationList?.annotationEntries ?: emptyList()

    @Throws(IncorrectOperationException::class)
    override fun setName(name: String): PsiElement {
        val result = super.setName(name)
        val willBeScript = name.endsWith(KotlinFileType.SCRIPT_EXTENSION)
        if (isScript() != willBeScript) {
            FileContentUtilCore.reparseFiles(listOfNotNull(virtualFile))
        }
        return result
    }

    override fun getPsiOrParent(): KtElement = this

    @Suppress("unused") //keep for compatibility with potential plugins
    fun shouldChangeModificationCount(@Suppress("UNUSED_PARAMETER") place: PsiElement): Boolean {
        // Modification count for Kotlin files is tracked entirely by KotlinCodeBlockModificationListener
        return false
    }
}

private fun KtImportList.computeHasImportAlias(): Boolean {
    val stub = greenStub
    if (stub != null) {
        return stub.childrenStubs.any {
            it is KotlinImportDirectiveStub && it.findChildStubByType(KtStubBasedElementTypes.IMPORT_ALIAS) != null
        }
    }

    return node.children().any {
        val psi = it.psi
        psi is KtImportDirective && psi.alias != null
    }
}