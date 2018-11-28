/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.stubs.ConstantValueKind
import org.jetbrains.kotlin.psi.stubs.KotlinConstantExpressionStub
import org.jetbrains.kotlin.psi.stubs.elements.KtConstantExpressionElementType

class KotlinConstantExpressionStubImpl(
    parent: StubElement<out PsiElement>?,
    elementType: KtConstantExpressionElementType,
    private val kind: ConstantValueKind,
    private val value: StringRef
) : KotlinStubBaseImpl<KtConstantExpression>(parent, elementType), KotlinConstantExpressionStub {
    override fun kind(): ConstantValueKind = kind
    override fun value(): String = StringRef.toString(value)
}