/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightTypeElement
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.light.classes.symbol.toArrayIfNotEmptyOrDefault
import org.jetbrains.kotlin.psi.KtElement
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

internal class SymbolPsiArrayInitializerMemberValue(
    override val kotlinOrigin: KtElement?,
    private val lightParent: PsiElement,
    private val arguments: (SymbolPsiArrayInitializerMemberValue) -> List<PsiAnnotationMemberValue>
) : KtLightElementBase(lightParent), PsiArrayInitializerMemberValue {
    @Volatile
    private var cachedArguments: List<PsiAnnotationMemberValue>? = null

    override fun getInitializers(): Array<PsiAnnotationMemberValue> =
        getOrCompareArguments().toArrayIfNotEmptyOrDefault(PsiAnnotationMemberValue.EMPTY_ARRAY)

    private fun getOrCompareArguments(): List<PsiAnnotationMemberValue> {
        cachedArguments?.let { return it }

        val memberValues = arguments(this)
        fieldUpdater.compareAndSet(this, null, memberValues)
        return getOrCompareArguments()
    }

    override fun getParent(): PsiElement = lightParent
    override fun isPhysical(): Boolean = false

    override fun getText(): String = "{" + getOrCompareArguments().joinToString { it.text } + "}"

    companion object {
        private val fieldUpdater = AtomicReferenceFieldUpdater.newUpdater(
            /* tclass = */ SymbolPsiArrayInitializerMemberValue::class.java,
            /* vclass = */ List::class.java,
            /* fieldName = */ "cachedArguments",
        )
    }
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

internal class SymbolPsiClassObjectAccessExpression(
    override val kotlinOrigin: KtElement?,
    lightParent: PsiElement,
    private val psiType: PsiType,
) : SymbolPsiAnnotationMemberValue(kotlinOrigin, lightParent), PsiClassObjectAccessExpression {
    override fun getType(): PsiType = psiType
    override fun getOperand(): PsiTypeElement = LightTypeElementWithParent(this, type)
    override fun getText(): String = type.getCanonicalText(false) + ".class"
}

private class LightTypeElementWithParent(private val lightParent: PsiElement, type: PsiType) : LightTypeElement(lightParent.manager, type) {
    override fun getParent(): PsiElement = lightParent
}

internal class SymbolPsiReference(
    override val kotlinOrigin: KtElement?,
    lightParent: PsiElement,
    private val psiReference: PsiReferenceExpression,
) : SymbolPsiAnnotationMemberValue(kotlinOrigin, lightParent),
    PsiReferenceExpression, PsiJavaReference by psiReference {
    override fun getText(): String = psiReference.text

    override fun getReferenceNameElement(): PsiElement? = psiReference.referenceNameElement
    override fun getParameterList(): PsiReferenceParameterList? = psiReference.parameterList
    override fun getTypeParameters(): Array<PsiType> = psiReference.typeParameters
    override fun isQualified(): Boolean = psiReference.isQualified
    override fun getQualifiedName(): String? = psiReference.qualifiedName
    override fun getQualifier(): PsiElement? = psiReference.qualifier
    override fun getReferenceName(): String? = psiReference.referenceName
    override fun <T> getCopyableUserData(key: Key<T>): T? = null
    override fun <T> putCopyableUserData(key: Key<T>, value: T?) {}
    override fun getQualifierExpression(): PsiExpression? = psiReference.qualifierExpression
    override fun bindToElementViaStaticImport(qualifierClass: PsiClass): PsiElement =
        cannotModify()

    override fun setQualifierExpression(newQualifier: PsiExpression?) {
        cannotModify()
    }

    override fun getType(): PsiType? = psiReference.type
}

internal class SymbolPsiLiteral(
    override val kotlinOrigin: KtElement?,
    lightParent: PsiElement,
    private val psiLiteral: PsiLiteral,
) : SymbolPsiAnnotationMemberValue(kotlinOrigin, lightParent), PsiLiteral {
    override fun getValue(): Any? = psiLiteral.value
    override fun getText(): String = psiLiteral.text
}
