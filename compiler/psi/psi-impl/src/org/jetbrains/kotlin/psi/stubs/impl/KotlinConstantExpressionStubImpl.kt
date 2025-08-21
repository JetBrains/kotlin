/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.ConstantValueKind
import org.jetbrains.kotlin.psi.stubs.KotlinConstantExpressionStub
import org.jetbrains.kotlin.psi.utils.toConstantExpressionElementType

@OptIn(KtImplementationDetail::class)
class KotlinConstantExpressionStubImpl(
    parent: StubElement<*>?,
    override val kind: ConstantValueKind,
    private val valueRef: StringRef,
) : KotlinStubBaseImpl<KtConstantExpression>(
    parent = parent,
    elementType = kind.toConstantExpressionElementType(),
), KotlinConstantExpressionStub {
    override val value: String get() = valueRef.string

    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinConstantExpressionStubImpl = KotlinConstantExpressionStubImpl(
        parent = newParent,
        kind = kind,
        valueRef = valueRef,
    )
}
