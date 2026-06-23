/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.ConstantValueKind
import org.jetbrains.kotlin.psi.stubs.StubUtils
import org.jetbrains.kotlin.psi.stubs.elements.KtConstantExpressionElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.impl.KotlinConstantExpressionStubImpl
import org.jetbrains.kotlin.psi.utils.toConstantValueKind

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinConstantExpressionStubFactory : StubElementFactory<KotlinConstantExpressionStubImpl, KtConstantExpression> {
    @OptIn(KtImplementationDetail::class)
    override fun shouldCreateStub(node: ASTNode): Boolean {
        return StubUtils.isDeclaredInsideValueArgument(node) && KtStubElementType.shouldCreateStubDependingOnParent(node)
    }

    override fun createStub(psi: KtConstantExpression, parentStub: StubElement<out PsiElement>?): KotlinConstantExpressionStubImpl {
        val elementType = psi.node.elementType as? KtConstantExpressionElementType
            ?: throw IllegalStateException("Stub element type is expected for constant")

        val value = psi.text

        return KotlinConstantExpressionStubImpl(
            parentStub,
            elementType.toConstantValueKind(),
            StringRef.fromString(value),
        )
    }

    override fun createPsi(stub: KotlinConstantExpressionStubImpl): KtConstantExpression = KtConstantExpression(stub)
}

internal class KotlinConstantExpressionStubSerializer(
    private val elementType: KtStubElementType<KotlinConstantExpressionStubImpl, *>,
) : StubSerializer<KotlinConstantExpressionStubImpl> {
    override fun getExternalId(): String = elementType.conventionalExternalId

    override fun serialize(stub: KotlinConstantExpressionStubImpl, dataStream: StubOutputStream) {
        dataStream.writeVarInt(stub.kind.ordinal)
        dataStream.writeName(stub.value)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinConstantExpressionStubImpl {
        val kindOrdinal = dataStream.readVarInt()
        val value = dataStream.readName() ?: StringRef.fromString("")

        val valueKind = ConstantValueKind.entries[kindOrdinal]

        return KotlinConstantExpressionStubImpl(
            parentStub,
            valueKind,
            value,
        )
    }

    override fun indexStub(stub: KotlinConstantExpressionStubImpl, sink: IndexSink) {
        // not indexed
    }
}
