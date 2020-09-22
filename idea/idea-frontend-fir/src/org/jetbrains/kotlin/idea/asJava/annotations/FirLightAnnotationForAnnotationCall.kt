/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.asJava

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtAnnotationCall
import org.jetbrains.kotlin.psi.KtCallElement

internal class FirLightAnnotationForAnnotationCall(
    private val annotationCall: KtAnnotationCall,
    parent: PsiElement,
) : FirLightAbstractAnnotation(parent) {

    override val kotlinOrigin: KtCallElement? = annotationCall.psi

    override fun getQualifiedName(): String? = annotationCall.classId?.asSingleFqName()?.asString()

    override fun getName(): String? = qualifiedName

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is FirLightAnnotationForAnnotationCall &&
                        kotlinOrigin == other.kotlinOrigin &&
                        annotationCall == other.annotationCall)

    override fun hashCode(): Int = kotlinOrigin.hashCode()

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        basicIsEquivalentTo(this, another as? PsiAnnotation)
}