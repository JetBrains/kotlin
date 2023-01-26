/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotationParameterList
import org.jetbrains.kotlin.analysis.api.annotations.KtNamedAnnotationValue
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.psi.KtElement

internal sealed class SymbolLightAbstractAnnotationParameterList(
    parent: SymbolLightAbstractAnnotation,
) : KtLightElementBase(parent), PsiAnnotationParameterList {
    override val kotlinOrigin: KtElement?
        get() = (parent as SymbolLightAbstractAnnotation).kotlinOrigin?.valueArgumentList

    override fun equals(other: Any?): Boolean = other === this || other is SymbolLightLazyAnnotationParameterList && other.parent == parent
    override fun hashCode(): Int = parent.hashCode()
}

internal fun SymbolLightAbstractAnnotation.symbolLightAnnotationParameterList(
    arguments: List<KtNamedAnnotationValue> = emptyList(),
): SymbolLightAbstractAnnotationParameterList = if (arguments.isNotEmpty()) {
    SymbolLightLazyAnnotationParameterList(this, lazyOf(arguments))
} else {
    SymbolLightEmptyAnnotationParameterList(this)
}
