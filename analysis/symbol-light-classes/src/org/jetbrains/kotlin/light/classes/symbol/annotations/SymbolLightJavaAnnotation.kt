/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotationParameterList
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.annotations.KtNamedAnnotationValue
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.psi.KtCallElement

internal open class SymbolLightJavaAnnotation(
    val originalLightAnnotation: SymbolLightLazyAnnotation,
    private val javaQualifier: String,
    owner: PsiElement,
    private val argumentsComputer: SymbolLightJavaAnnotation.() -> List<KtNamedAnnotationValue>,
) : SymbolLightAbstractAnnotation(owner) {
    override fun createReferenceInformationProvider(): ReferenceInformationProvider = ReferenceInformationHolder(
        referenceName = javaQualifier.substringAfterLast('.'),
    )

    override val kotlinOrigin: KtCallElement? get() = originalLightAnnotation.kotlinOrigin

    override fun equals(other: Any?): Boolean = other === this ||
            other is SymbolLightJavaAnnotation &&
            other.javaQualifier == javaQualifier &&
            other.originalLightAnnotation == originalLightAnnotation

    override fun hashCode(): Int = javaQualifier.hashCode()

    override fun getQualifiedName(): String = javaQualifier

    private val _parameterList: PsiAnnotationParameterList by lazyPub {
        symbolLightAnnotationParameterList { argumentsComputer() }
    }

    override fun getParameterList(): PsiAnnotationParameterList = _parameterList
}
