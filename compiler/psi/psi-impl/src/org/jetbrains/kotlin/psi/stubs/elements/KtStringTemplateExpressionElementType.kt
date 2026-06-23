/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.StubElementFactory
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.stubs.StubUtils
import org.jetbrains.kotlin.psi.stubs.factory.KotlinPlaceHolderStubFactory
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderStubImpl

class KtStringTemplateExpressionElementType(@NonNls debugName: String) :
    KtPlaceHolderStubElementType<KtStringTemplateExpression>(debugName, KtStringTemplateExpression::class.java) {

    private val stubFactory = object : KotlinPlaceHolderStubFactory<KtStringTemplateExpression>(this) {
        override fun shouldCreateStub(node: ASTNode): Boolean {
            return StubUtils.isDeclaredInsideValueArgument(node) && super.shouldCreateStub(node)
        }
    }

    @KtImplementationDetail
    override fun getStubFactory(): StubElementFactory<KotlinPlaceHolderStubImpl<KtStringTemplateExpression>, KtStringTemplateExpression> =
        stubFactory
}
