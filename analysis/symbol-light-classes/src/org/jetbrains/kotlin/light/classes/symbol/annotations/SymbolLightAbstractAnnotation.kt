/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.classes.cannotModify
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.light.classes.symbol.codeReferences.SymbolLightPsiJavaCodeReferenceElementWithNoReference
import org.jetbrains.kotlin.light.classes.symbol.codeReferences.SymbolLightPsiJavaCodeReferenceElementWithReference
import org.jetbrains.kotlin.psi.*

internal abstract class SymbolLightAbstractAnnotation(parent: PsiElement) :
    KtLightElementBase(parent), PsiAnnotation, KtLightElement<KtCallElement, PsiAnnotation> {

    override fun getOwner() = parent as? PsiAnnotationOwner

    private val KtExpression.nameReference: KtNameReferenceExpression?
        get() = when (this) {
            is KtConstructorCalleeExpression -> constructorReferenceExpression as? KtNameReferenceExpression
            else -> this as? KtNameReferenceExpression
        }

    private val _nameReferenceElement: PsiJavaCodeReferenceElement by lazyPub {
        val ktElement = kotlinOrigin?.navigationElement ?: this
        val reference = (kotlinOrigin as? KtAnnotationEntry)?.typeReference?.reference
            ?: (kotlinOrigin?.calleeExpression?.nameReference)?.references?.firstOrNull()

        if (reference != null) SymbolLightPsiJavaCodeReferenceElementWithReference(ktElement, reference)
        else SymbolLightPsiJavaCodeReferenceElementWithNoReference(ktElement)
    }

    override fun getNameReferenceElement(): PsiJavaCodeReferenceElement = _nameReferenceElement

    private class SymbolAnnotationParameterList(parent: PsiAnnotation) : KtLightElementBase(parent), PsiAnnotationParameterList {
        override val kotlinOrigin: KtElement? = null
        override fun getAttributes(): Array<PsiNameValuePair> = emptyArray() //TODO()
    }

    private val annotationParameterList: PsiAnnotationParameterList = SymbolAnnotationParameterList(this)

    override fun getParameterList(): PsiAnnotationParameterList = annotationParameterList

    override fun delete() {
        kotlinOrigin?.delete()
    }

    override fun toString() = "@$qualifiedName"

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

    override fun <T : PsiAnnotationMemberValue?> setDeclaredAttributeValue(attributeName: String?, value: T?) = cannotModify()

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitAnnotation(this)
        } else {
            visitor.visitElement(this)
        }
    }
}