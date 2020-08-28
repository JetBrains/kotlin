/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.KtContractEffect
import org.jetbrains.kotlin.psi.stubs.KotlinContractEffectStub
import org.jetbrains.kotlin.psi.stubs.elements.KtContractEffectElementType

class KotlinContractEffectStubImpl(
    parent: StubElement<out PsiElement>?,
    elementType: KtContractEffectElementType
) : KotlinPlaceHolderStubImpl<KtContractEffect>(parent, elementType), KotlinContractEffectStub {
}