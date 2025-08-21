/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.stubs.KotlinClassLiteralExpressionStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KotlinClassLiteralExpressionStubImpl(
    parent: StubElement<out PsiElement>?
) : KotlinStubBaseImpl<KtClassLiteralExpression>(parent, KtStubElementTypes.CLASS_LITERAL_EXPRESSION), KotlinClassLiteralExpressionStub