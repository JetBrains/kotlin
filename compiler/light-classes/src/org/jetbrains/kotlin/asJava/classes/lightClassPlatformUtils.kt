/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.PsiElement
import com.intellij.psi.augment.PsiAugmentProvider

internal fun <Psi : PsiElement> collectAugments(element: PsiElement, type: Class<out Psi>): List<Psi> {
    return PsiAugmentProvider.collectAugments(element, type, null)
}
