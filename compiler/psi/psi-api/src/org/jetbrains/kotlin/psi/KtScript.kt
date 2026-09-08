/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.psi.stubs.KotlinScriptStub
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets

/**
 * Represents a Kotlin script file containing top-level statements and declarations.
 *
 * ### Example:
 *
 * // script.kts
 * ```kotlin
 * val x = 1
 * println(x)
 * ```
 *
 * Note: this class is not intended to be extended and is marked `open` solely for backward compatibility.
 */
@SubclassOptInRequired(KtImplementationDetail::class)
open class KtScript : KtNamedDeclarationStub<KotlinScriptStub>, KtDeclarationContainer {
    @KtImplementationDetail
    constructor(node: ASTNode) : super(node)

    @KtImplementationDetail
    constructor(stub: KotlinScriptStub) : super(stub, KtNodeTypes.SCRIPT)

    override fun getFqName(): FqName {
        val stub = greenStub
        if (stub != null) {
            return stub.fqName
        }

        val containingKtFile = containingKtFile
        val fileName = containingKtFile.name

        @OptIn(KtExperimentalApi::class)
        val fileBasedName = if (isReplSnippet) {
            NameUtils.getSnippetTargetClassName(fileName)
        } else {
            NameUtils.getScriptNameForFile(fileName)
        }

        return containingKtFile.packageFqName.child(fileBasedName)
    }

    /**
     * The [ClassId] of the class that represents this script if it [isReplSnippet].
     */
    @KtExperimentalApi
    val replSnippetClassId: ClassId?
        get() = if (isReplSnippet) {
            ClassId.topLevel(fqName)
        } else {
            null
        }

    override fun getName(): String = fqName.shortName().asString()

    /**
     * The block holding the script's top-level statements and declarations.
     */
    val blockExpression: KtBlockExpression
        get() = findNotNullChildByClass(KtBlockExpression::class.java)

    override fun getDeclarations(): List<KtDeclaration> {
        return stub?.getChildrenByType(KtTokenSets.DECLARATION_TYPES, KtDeclaration.ARRAY_FACTORY)?.toList()
            ?: PsiTreeUtil.getChildrenOfTypeAsList(this.blockExpression, KtDeclaration::class.java)
    }

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitScript(this, data)
    }

    /**
     * Determines whether a [KtScript] should be treated as a REPL snippet or not.
     */
    @KtExperimentalApi
    @OptIn(KtImplementationDetail::class)
    val isReplSnippet: Boolean
        get() = greenStub?.isReplSnippet ?: containingKtFile.isMarkedAsReplSnippet

    /**
     * Marks the [KtScript] as a REPL snippet, so it is treated by the compiler accordingly.
     */
    @KtNonPublicApi
    fun markAsReplSnippet() {
        containingKtFile.putCopyableUserData(REPL_SNIPPET_KEY, true)
        containingKtFile.replSnippetMarkFile.putUserData(REPL_SNIPPET_KEY, true)
    }
}

private val REPL_SNIPPET_KEY = Key.create<Boolean>("REPL_SNIPPET")

/**
 * Being a REPL snippet is a property of the whole file, so the mark is stored on the file and not on [KtScript] itself,
 * as the script element is recreated on every tree reloading.
 *
 * The mark is duplicated on purpose:
 * - copyable data on the file survives [com.intellij.psi.PsiElement.copy]
 * - data on [replSnippetMarkFile] survives tree reloading and is visible during stub building,
 *   which builds its own tree from the file content
 *
 * @see com.intellij.psi.impl.source.PsiFileImpl.loadTreeElement
 * @see com.intellij.psi.impl.source.PsiFileImpl.getVirtualFile
 */
private val KtFile.isMarkedAsReplSnippet: Boolean
    get() = getCopyableUserData(REPL_SNIPPET_KEY) == true || replSnippetMarkFile.getUserData(REPL_SNIPPET_KEY) == true

/**
 * [com.intellij.psi.impl.source.PsiFileImpl.getVirtualFile] returns the original file during stub building,
 * so the mark is visible there as well.
 * Non-physical files have no such file, so the view provider one is used as a fallback.
 */
private val KtFile.replSnippetMarkFile: VirtualFile
    get() = virtualFile ?: viewProvider.virtualFile
