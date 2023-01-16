/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.psi.KtElement

internal class SymbolPsiArrayInitializerMemberValue(
    override val kotlinOrigin: KtElement?,
    private val lightParent: PsiElement,
    private val arguments: (SymbolPsiArrayInitializerMemberValue) -> List<PsiAnnotationMemberValue>
) : KtLightElementBase(lightParent), PsiArrayInitializerMemberValue {

    override fun getInitializers(): Array<PsiAnnotationMemberValue> = arguments(this).toTypedArray()

    override fun getParent(): PsiElement = lightParent
    override fun isPhysical(): Boolean = false

    override fun getText(): String = "{" + initializers.joinToString { it.text } + "}"
}

internal abstract class SymbolPsiAnnotationMemberValue(
    override val kotlinOrigin: KtElement?,
    private val lightParent: PsiElement,
) : KtLightElementBase(lightParent), PsiAnnotationMemberValue {

    override fun getParent(): PsiElement = lightParent
    override fun isPhysical(): Boolean = false
}

internal class SymbolPsiExpression(
    override val kotlinOrigin: KtElement?,
    lightParent: PsiElement,
    private val psiExpression: PsiExpression,
) : SymbolPsiAnnotationMemberValue(kotlinOrigin, lightParent), PsiExpression {
    override fun getType(): PsiType? = psiExpression.type
    override fun getText(): String = psiExpression.text
}

internal class SymbolPsiLiteral(
    override val kotlinOrigin: KtElement?,
    lightParent: PsiElement,
    private val psiLiteral: PsiLiteral,
) : SymbolPsiAnnotationMemberValue(kotlinOrigin, lightParent), PsiLiteral {
    override fun getValue(): Any? = psiLiteral.value
    override fun getText(): String = psiLiteral.text
}
