/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.psi.*
import org.jetbrains.kotlin.psi.KtCallElement

abstract class KtLightAbstractAnnotation(parent: PsiElement) :
    KtLightElementBase(parent), PsiAnnotation, KtLightElement<KtCallElement, PsiAnnotation> {

    abstract override fun getNameReferenceElement(): PsiJavaCodeReferenceElement?

    override fun getOwner(): PsiAnnotationOwner? = parent as? PsiAnnotationOwner

    abstract override fun getParameterList(): PsiAnnotationParameterList

    open fun fqNameMatches(fqName: String): Boolean = qualifiedName == fqName
}