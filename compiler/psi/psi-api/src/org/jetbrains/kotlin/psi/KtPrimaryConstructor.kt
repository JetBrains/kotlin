/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtNonPublicApi::class)

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.stubs.KotlinConstructorStub

/**
 * Represents a primary constructor explicitly declared in a class header.
 *
 * ### Example:
 *
 * ```kotlin
 * class Person constructor(val name: String)
 * //           ^___________________________^
 * ```
 */
class KtPrimaryConstructor : KtConstructor<KtPrimaryConstructor> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: KotlinConstructorStub<KtPrimaryConstructor>) : super(stub, KtStubBasedElementTypes.PRIMARY_CONSTRUCTOR)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D) = visitor.visitPrimaryConstructor(this, data)

    override fun getContainingClassOrObject() = parent as KtClassOrObject

    @Deprecated(
        "Use removeRedundantConstructorKeyword() instead",
        ReplaceWith(
            "this.removeRedundantConstructorKeyword()",
            "org.jetbrains.kotlin.idea.base.psi.removeRedundantConstructorKeyword",
        ),
    )
    fun removeRedundantConstructorKeywordAndSpace() {
        KtPsiMutationService.getInstance().removeRedundantConstructorKeyword(this)
    }

    @Deprecated(
        "Use addModifierKeyword(modifier) instead",
        ReplaceWith("this.addModifierKeyword(modifier)", "org.jetbrains.kotlin.idea.base.psi.addModifierKeyword"),
    )
    override fun addModifier(modifier: KtModifierKeywordToken) {
        KtPsiMutationService.getInstance().addModifierKeyword(this, modifier)
    }

    @Deprecated(
        "Use removeModifierKeyword(modifier) instead",
        ReplaceWith("this.removeModifierKeyword(modifier)", "org.jetbrains.kotlin.idea.base.psi.removeModifierKeyword"),
    )
    override fun removeModifier(modifier: KtModifierKeywordToken) {
        KtPsiMutationService.getInstance().removeModifierKeyword(this, modifier)
    }

    @Deprecated(
        "Use addAnnotation(annotationEntry) instead",
        ReplaceWith("this.addAnnotation(annotationEntry)", "org.jetbrains.kotlin.idea.base.psi.addAnnotation"),
    )
    override fun addAnnotationEntry(annotationEntry: KtAnnotationEntry): KtAnnotationEntry =
        KtPsiMutationService.getInstance().addAnnotation(this, annotationEntry)

    override fun mayHaveContract(): Boolean = false
}
