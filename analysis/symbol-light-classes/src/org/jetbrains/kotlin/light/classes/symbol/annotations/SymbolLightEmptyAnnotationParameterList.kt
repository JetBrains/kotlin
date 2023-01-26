/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol.annotations

import com.intellij.psi.PsiNameValuePair

internal class SymbolLightEmptyAnnotationParameterList(
    parent: SymbolLightAbstractAnnotation,
) : SymbolLightAbstractAnnotationParameterList(parent) {
    override fun getAttributes(): Array<PsiNameValuePair> = PsiNameValuePair.EMPTY_ARRAY
}
