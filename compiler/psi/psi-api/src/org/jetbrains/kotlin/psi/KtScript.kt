/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.Key
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.NameUtils
import org.jetbrains.kotlin.psi.stubs.KotlinScriptStub

/**
 * Represents a Kotlin script file containing top-level statements and declarations.
 * 
 * ### Example:
 *
 * // script.kts
 * ```kotlin
 * val x = 1 println(x)
 * ```
 *
 * Note: this class is not intended to be extended and is marked `open` solely for backward compatibility.
 */
open class KtScript : KtNamedDeclarationStub<KotlinScriptStub>, KtDeclarationContainer {
    constructor(node: ASTNode) : super(node)

    constructor(stub: KotlinScriptStub) : super(stub, KtStubBasedElementTypes.SCRIPT)

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

    override fun getName(): String = fqName.shortName().asString()

    val blockExpression: KtBlockExpression
        get() = findNotNullChildByClass(KtBlockExpression::class.java)

    override fun getDeclarations(): List<KtDeclaration> {
        return PsiTreeUtil.getChildrenOfTypeAsList(this.blockExpression, KtDeclaration::class.java)
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
        get() = greenStub?.isReplSnippet ?: (this.getCopyableUserData(REPL_SNIPPET_KEY) == true)

    /**
     * Marks the [KtScript] as a REPL snippet, so it is treated by the compiler accordingly.
     */
    @KtNonPublicApi
    fun markAsReplSnippet() {
        putCopyableUserData(REPL_SNIPPET_KEY, true)
    }
}

private val REPL_SNIPPET_KEY = Key.create<Boolean>("REPL_SNIPPET")
