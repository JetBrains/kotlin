/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiAnnotationParameterList
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.PsiImplUtil
import org.jetbrains.kotlin.analysis.api.annotations.KtNamedConstantValue
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.psi.KtCallElement

internal class FirLightSimpleAnnotation(
    private val fqName: String?,
    parent: PsiElement,
    private val arguments: List<KtNamedConstantValue> = listOf(),
    override val kotlinOrigin: KtCallElement? = null,
) : FirLightAbstractAnnotation(parent) {

    override fun getQualifiedName(): String? = fqName

    override fun getName(): String? = fqName

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FirLightSimpleAnnotation && fqName == other.fqName && parent == other.parent)

    override fun hashCode(): Int = fqName.hashCode()

    override fun findAttributeValue(attributeName: String?): PsiAnnotationMemberValue? =
        PsiImplUtil.findAttributeValue(this, attributeName)

    override fun findDeclaredAttributeValue(attributeName: String?) =
        PsiImplUtil.findDeclaredAttributeValue(this, attributeName)

    private val _parameterList: PsiAnnotationParameterList by lazyPub {
        FirAnnotationParameterList(this@FirLightSimpleAnnotation, arguments)
    }

    override fun getParameterList(): PsiAnnotationParameterList = _parameterList

}
