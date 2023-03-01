/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.stubs.KotlinCollectionLiteralExpressionStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KotlinCollectionLiteralExpressionStubImpl(
    parent: StubElement<out PsiElement>?
) : KotlinStubBaseImpl<KtCollectionLiteralExpression>(parent, KtStubElementTypes.COLLECTION_LITERAL_EXPRESSION),
    KotlinCollectionLiteralExpressionStub