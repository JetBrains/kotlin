/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiNameValuePair
import org.jetbrains.kotlin.analysis.api.annotations.KtNamedAnnotationValue
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.light.classes.symbol.toArrayIfNotEmptyOrDefault

internal class SymbolLightLazyAnnotationParameterList(
    parent: SymbolLightAbstractAnnotation,
    private val lazyArguments: Lazy<List<KtNamedAnnotationValue>>,
) : SymbolLightAbstractAnnotationParameterList(parent) {
    private val _attributes: Collection<PsiNameValuePair> by lazyPub {
        val attributes = lazyArguments.value.map {
            SymbolNameValuePairForAnnotationArgument(it, this)
        }

        attributes
    }

    override fun getAttributes(): Array<PsiNameValuePair> = _attributes.toArrayIfNotEmptyOrDefault(PsiNameValuePair.EMPTY_ARRAY)
}
