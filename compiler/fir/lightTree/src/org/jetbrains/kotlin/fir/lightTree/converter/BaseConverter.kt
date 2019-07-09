/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.*

open class BaseConverter(
    session: FirSession,
    private val tree: FlyweightCapableTreeStructure<LighterASTNode>
) {
    protected val implicitUnitType = FirImplicitUnitTypeRef(session, null)
    protected val implicitAnyType = FirImplicitAnyTypeRef(session, null)
    protected val implicitEnumType = FirImplicitEnumTypeRef(session, null)
    protected val implicitAnnotationType = FirImplicitAnnotationTypeRef(session, null)
    protected val implicitType = FirImplicitTypeRefImpl(session, null)

    protected fun LighterASTNode?.getChildNodesByType(type: IElementType): List<LighterASTNode> {
        return this?.forEachChildrenReturnList { node, container ->
            when (node.tokenType) {
                type -> container += node
            }
        } ?: listOf()
    }

    protected fun LighterASTNode?.getChildrenAsArray(): Array<LighterASTNode?> {
        if (this == null) return arrayOf()

        val kidsRef = Ref<Array<LighterASTNode?>>()
        tree.getChildren(this, kidsRef)
        return kidsRef.get()
    }

    protected inline fun LighterASTNode.forEachChildren(vararg skipTokens: KtToken, f: (LighterASTNode) -> Unit) {
        val kidsArray = this.getChildrenAsArray()
        for (kid in kidsArray) {
            if (kid == null) break
            val tokenType = kid.tokenType
            if (COMMENTS.contains(tokenType) || tokenType == WHITE_SPACE || tokenType == SEMICOLON || tokenType in skipTokens) continue
            f(kid)
        }
    }

    protected inline fun <T> LighterASTNode.forEachChildrenReturnList(f: (LighterASTNode, MutableList<T>) -> Unit): List<T> {
        val kidsArray = this.getChildrenAsArray()

        val container = mutableListOf<T>()
        for (kid in kidsArray) {
            if (kid == null) break
            val tokenType = kid.tokenType
            if (COMMENTS.contains(tokenType) || tokenType == WHITE_SPACE || tokenType == SEMICOLON) continue
            f(kid, container)
        }

        return container
    }
}