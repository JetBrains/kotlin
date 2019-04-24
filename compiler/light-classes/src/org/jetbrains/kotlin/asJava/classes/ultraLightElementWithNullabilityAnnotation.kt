/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiType
import org.jetbrains.kotlin.asJava.elements.KtLightDeclaration
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.types.KotlinType

interface KtUltraLightElementWithNullabilityAnnotation<out T : KtDeclaration, out D : PsiModifierListOwner> : KtLightDeclaration<T, D>,
    PsiModifierListOwner {
    val kotlinTypeForNullabilityAnnotation: KotlinType?
    val psiTypeForNullabilityAnnotation: PsiType?
}