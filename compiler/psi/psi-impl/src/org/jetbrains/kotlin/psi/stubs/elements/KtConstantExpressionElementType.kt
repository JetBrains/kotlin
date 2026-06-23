/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElementFactory
import com.intellij.psi.stubs.StubSerializer
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.ConstantValueKind
import org.jetbrains.kotlin.psi.stubs.KotlinConstantExpressionStub
import org.jetbrains.kotlin.psi.stubs.factory.KotlinConstantExpressionStubFactory
import org.jetbrains.kotlin.psi.stubs.factory.KotlinConstantExpressionStubSerializer
import org.jetbrains.kotlin.psi.stubs.impl.KotlinConstantExpressionStubImpl
import org.jetbrains.kotlin.psi.utils.toConstantExpressionElementType
import org.jetbrains.kotlin.psi.utils.toConstantValueKind

class KtConstantExpressionElementType(@NonNls debugName: String) :
    KtStubElementType<KotlinConstantExpressionStubImpl, KtConstantExpression>(
        debugName,
        KtConstantExpression::class.java,
        KotlinConstantExpressionStub::class.java,
    ) {
    // Unlike the single-constant element types, the same class backs several constants (NULL, INTEGER_CONSTANT, ...),
    // each with its own external id, so the serializer is per-instance rather than a shared object (KT-78356).
    private val stubSerializer = KotlinConstantExpressionStubSerializer(this)

    @KtImplementationDetail
    override fun getStubFactory(): StubElementFactory<KotlinConstantExpressionStubImpl, KtConstantExpression> =
        KotlinConstantExpressionStubFactory

    @KtImplementationDetail
    override fun getStubSerializer(): StubSerializer<KotlinConstantExpressionStubImpl> = stubSerializer

    companion object {
        @Deprecated(
            "Use ConstantValueKind.toConstantExpressionElementType() instead",
            ReplaceWith("kind.toConstantExpressionElementType()", "org.jetbrains.kotlin.psi.utils.toConstantExpressionElementType")
        )
        fun kindToConstantElementType(kind: ConstantValueKind): KtConstantExpressionElementType {
            return kind.toConstantExpressionElementType() as KtConstantExpressionElementType
        }

        @Deprecated(
            "Use KtConstantExpressionElementType.toConstantValueKind() instead",
            ReplaceWith("elementType.toConstantValueKind()", "org.jetbrains.kotlin.psi.utils.toConstantValueKind")
        )
        private fun constantElementTypeToKind(elementType: KtConstantExpressionElementType): ConstantValueKind {
            return elementType.toConstantValueKind()
        }
    }
}
