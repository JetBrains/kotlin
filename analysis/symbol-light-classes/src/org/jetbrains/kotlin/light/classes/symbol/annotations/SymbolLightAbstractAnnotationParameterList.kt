/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotationParameterList
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.analysis.api.annotations.KtNamedAnnotationValue
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.psi.KtElement

internal sealed class SymbolLightAbstractAnnotationParameterList(
    parent: SymbolLightAbstractAnnotation,
) : KtLightElementBase(parent), PsiAnnotationParameterList {
    override val kotlinOrigin: KtElement? get() = (parent as SymbolLightAbstractAnnotation).kotlinOrigin?.valueArgumentList

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitAnnotationParameterList(this)
        } else {
            visitor.visitElement(this)
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun SymbolLightAbstractAnnotation.symbolLightAnnotationParameterList(): SymbolLightAbstractAnnotationParameterList {
    return SymbolLightEmptyAnnotationParameterList(this)
}

internal fun SymbolLightAbstractAnnotation.symbolLightAnnotationParameterList(
    arguments: List<KtNamedAnnotationValue>,
): SymbolLightAbstractAnnotationParameterList = if (arguments.isNotEmpty()) {
    SymbolLightLazyAnnotationParameterList(this, lazyOf(arguments))
} else {
    symbolLightAnnotationParameterList()
}

internal inline fun SymbolLightAbstractAnnotation.symbolLightAnnotationParameterList(
    crossinline argumentsComputer: () -> List<KtNamedAnnotationValue>,
): SymbolLightAbstractAnnotationParameterList = SymbolLightLazyAnnotationParameterList(this, lazyPub { argumentsComputer() })
