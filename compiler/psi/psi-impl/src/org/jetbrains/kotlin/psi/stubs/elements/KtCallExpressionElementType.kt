/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.StubElementFactory
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.stubs.StubUtils
import org.jetbrains.kotlin.psi.stubs.factory.KotlinPlaceHolderStubFactory
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderStubImpl

internal object KtCallExpressionElementType : KtPlaceHolderStubElementType<KtCallExpression>(
    "CALL_EXPRESSION", KtCallExpression::class.java
) {
    private val stubFactory = object : KotlinPlaceHolderStubFactory<KtCallExpression>(this) {
        override fun shouldCreateStub(node: ASTNode): Boolean {
            return StubUtils.isDeclaredInsideValueArgument(node) && super.shouldCreateStub(node)
        }
    }

    override fun getStubFactory(): StubElementFactory<KotlinPlaceHolderStubImpl<KtCallExpression>, KtCallExpression> =
        stubFactory
}
