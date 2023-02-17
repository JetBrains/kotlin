/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.contracts.description.ExpressionType
import org.jetbrains.kotlin.psi.KtContractExpression
import org.jetbrains.kotlin.psi.stubs.KotlinContractExpressionStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KotlinContractExpressionStubImpl(
    parent: StubElement<out PsiElement>?,
    val type: ExpressionType,
    val data: String
) : KotlinPlaceHolderStubImpl<KtContractExpression>(parent, KtStubElementTypes.CONTRACT_EXPRESSION), KotlinContractExpressionStub {
    override fun type(): ExpressionType {
        return type
    }

    override fun data(): String {
        return data
    }
}