/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.lookup.LookupElement

internal fun interface LookupElementWrapper {
    fun wrap(element: LookupElement): LookupElement
}


internal fun List<LookupElementWrapper>.wrap(element: LookupElement): LookupElement =
    fold(element) { currentElement, wrapper -> wrapper.wrap(currentElement) }
