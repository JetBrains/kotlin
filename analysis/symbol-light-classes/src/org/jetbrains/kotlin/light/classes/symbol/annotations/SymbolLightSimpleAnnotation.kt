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

internal class SymbolLightSimpleAnnotation(
    private val fqName: String?,
    parent: PsiElement,
    private val arguments: List<KtNamedAnnotationValue> = listOf(),
    override val kotlinOrigin: KtCallElement? = null,
) : SymbolLightAbstractAnnotation(parent) {
    override fun createReferenceInformationProvider(): ReferenceInformationProvider = ReferenceInformationHolder(
        referenceName = fqName?.substringAfterLast('.'),
    )

    override fun getQualifiedName(): String? = fqName

    override fun getName(): String? = fqName

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is SymbolLightSimpleAnnotation && fqName == other.fqName && parent == other.parent)

    override fun hashCode(): Int = fqName.hashCode()

    private val _parameterList: PsiAnnotationParameterList by lazyPub {
        symbolLightAnnotationParameterList(arguments)
    }

    override fun getParameterList(): PsiAnnotationParameterList = _parameterList
}
