/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(KtIdeApi::class)

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.KtNodeTypes
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
        val fileBasedName = NameUtils.getScriptNameForFile(containingKtFile.name)
        return containingKtFile.packageFqName.child(fileBasedName)
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
}
