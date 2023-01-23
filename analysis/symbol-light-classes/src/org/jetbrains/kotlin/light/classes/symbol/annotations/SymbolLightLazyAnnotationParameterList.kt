/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiAnnotationParameterList
import com.intellij.psi.PsiNameValuePair
import org.jetbrains.kotlin.analysis.api.annotations.KtNamedAnnotationValue
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.asJava.elements.KtLightElementBase
import org.jetbrains.kotlin.psi.KtElement

internal class SymbolLightLazyAnnotationParameterList(
    parent: SymbolLightAbstractAnnotation,
    private val lazyArguments: Lazy<List<KtNamedAnnotationValue>>,
) : KtLightElementBase(parent),
    PsiAnnotationParameterList {
    override val kotlinOrigin: KtElement?
        get() = (parent as SymbolLightAbstractAnnotation).kotlinOrigin?.valueArgumentList

    private val _attributes: Array<PsiNameValuePair> by lazyPub {
        val attributes = lazyArguments.value.map {
            SymbolNameValuePairForAnnotationArgument(it, this)
        }

        if (attributes.isEmpty()) PsiNameValuePair.EMPTY_ARRAY else attributes.toTypedArray()
    }

    override fun getAttributes(): Array<PsiNameValuePair> = _attributes

    override fun equals(other: Any?): Boolean = other === this || other is SymbolLightLazyAnnotationParameterList && other.parent == parent
    override fun hashCode(): Int = parent.hashCode()
}
