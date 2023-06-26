/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.diagnostics.getAncestors
import org.jetbrains.kotlin.diagnostics.nameIdentifier
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.elements.KtNameReferenceExpressionElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtTypeProjectionElementType
import org.jetbrains.kotlin.util.getChildren

/**
 * Service to answer source-related questions in generic fashion.
 * Shouldn't expose (receive or return) any specific source tree types
 */
interface SourceNavigator {

    fun FirTypeRef.isInConstructorCallee(): Boolean

    fun FirTypeRef.isInTypeConstraint(): Boolean

    fun KtSourceElement.getRawIdentifier(): CharSequence?

    fun FirDeclaration.getRawName(): String?

    fun FirValueParameterSymbol.isCatchElementParameter(): Boolean

    fun FirTypeRef.isRedundantNullable(): Boolean

    companion object {

        private val lightTreeInstance = LightTreeSourceNavigator()

        fun forElement(e: FirElement): SourceNavigator = forSource(e.source)

        fun forSource(e: KtSourceElement?): SourceNavigator = when (e) {
            is KtLightSourceElement -> lightTreeInstance
            is KtPsiSourceElement -> PsiSourceNavigator
            null -> lightTreeInstance //shouldn't matter
        }

        inline fun <R> FirElement.withNavigator(block: SourceNavigator.() -> R): R = with(forSource(this.source), block)
    }
}

open class LightTreeSourceNavigator : SourceNavigator {

    private fun <T> FirElement.withSource(f: (KtSourceElement) -> T): T? =
        source?.let { f(it) }

    override fun FirTypeRef.isInConstructorCallee(): Boolean = withSource { source ->
        source.treeStructure.getParent(source.lighterASTNode)?.tokenType == KtNodeTypes.CONSTRUCTOR_CALLEE
    } ?: false

    override fun FirTypeRef.isInTypeConstraint(): Boolean {
        val source = source ?: return false
        return source.treeStructure.getAncestors(source.lighterASTNode)
            .find { it.tokenType == KtNodeTypes.TYPE_CONSTRAINT || it.tokenType == KtNodeTypes.TYPE_PARAMETER }
            ?.tokenType == KtNodeTypes.TYPE_CONSTRAINT
    }

    override fun KtSourceElement.getRawIdentifier(): CharSequence? {
        return when (elementType) {
            is KtNameReferenceExpressionElementType, KtTokens.IDENTIFIER -> lighterASTNode.toString()
            is KtTypeProjectionElementType -> lighterASTNode.getChildren(treeStructure).last().toString()
            else -> null
        }
    }

    override fun FirDeclaration.getRawName(): String? {
        return source?.let { it.treeStructure.nameIdentifier(it.lighterASTNode)?.toString() }
    }

    override fun FirValueParameterSymbol.isCatchElementParameter(): Boolean {
        return source?.getParentOfParent()?.tokenType == KtNodeTypes.CATCH
    }

    override fun FirTypeRef.isRedundantNullable(): Boolean {
        val source = source ?: return false
        val ref = Ref<Array<LighterASTNode?>>()
        val firstChild = getNullableChild(source, source.lighterASTNode, ref) ?: return false
        return getNullableChild(source, firstChild, ref) != null
    }

    private fun getNullableChild(source: KtSourceElement, node: LighterASTNode, ref: Ref<Array<LighterASTNode?>>): LighterASTNode? {
        source.treeStructure.getChildren(node, ref)
        val firstChild = ref.get().firstOrNull() ?: return null
        return if (firstChild.tokenType != KtNodeTypes.NULLABLE_TYPE) null else firstChild
    }

    private fun KtSourceElement?.getParentOfParent(): LighterASTNode? {
        val source = this ?: return null
        var parent = source.treeStructure.getParent(source.lighterASTNode)
        parent?.let { parent = source.treeStructure.getParent(it) }
        return parent
    }
}

//by default psi tree can reuse light tree manipulations
object PsiSourceNavigator : LightTreeSourceNavigator() {

    //Swallows incorrect casts!!!
    private inline fun <reified P : PsiElement> FirElement.psi(): P? = source?.psi()

    private inline fun <reified P : PsiElement> KtSourceElement.psi(): P? {
        val psi = (this as? KtPsiSourceElement)?.psi
        return psi as? P
    }

    override fun FirTypeRef.isInConstructorCallee(): Boolean = psi<KtTypeReference>()?.parent is KtConstructorCalleeExpression

    override fun KtSourceElement.getRawIdentifier(): CharSequence? {
        val psi = psi<PsiElement>()
        return if (psi is KtNameReferenceExpression) {
            psi.getReferencedNameElement().node.chars
        } else if (psi is KtTypeProjection) {
            psi.typeReference?.typeElement?.text
        } else if (psi is LeafPsiElement && psi.elementType == KtTokens.IDENTIFIER) {
            psi.chars
        } else {
            null
        }
    }

    override fun FirDeclaration.getRawName(): String? {
        return (this.psi() as? PsiNameIdentifierOwner)?.nameIdentifier?.text
    }

    override fun FirValueParameterSymbol.isCatchElementParameter(): Boolean {
        return source?.psi<PsiElement>()?.parent?.parent is KtCatchClause
    }

    override fun FirTypeRef.isRedundantNullable(): Boolean {
        val source = source ?: return false
        val typeReference = (source.psi as? KtTypeReference) ?: return false
        val typeElement = typeReference.typeElement as? KtNullableType ?: return false
        return typeElement.innerType is KtNullableType
    }
}
