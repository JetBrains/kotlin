/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.ConstantValueKind
import org.jetbrains.kotlin.psi.stubs.impl.KotlinConstantExpressionStubImpl
import org.jetbrains.kotlin.psi.utils.toConstantExpressionElementType

/**
 * A factory for constant literal expressions of the given [kind]; each [ConstantValueKind] has its own element type.
 */
internal class KtConstantExpressionStubSerializingElementFactory(
    private val kind: ConstantValueKind,
) : KtStubSerializingElementFactory<KotlinConstantExpressionStubImpl, KtConstantExpression>(
    type = kind.toConstantExpressionElementType(),
) {

    override fun createPsi(stub: KotlinConstantExpressionStubImpl): KtConstantExpression = KtConstantExpression(stub)

    override fun createStub(
        psi: KtConstantExpression,
        parentStub: StubElement<*>?,
    ): KotlinConstantExpressionStubImpl = KotlinConstantExpressionStubImpl(
        parent = parentStub,
        kind = kind,
        valueRef = StringRef.fromString(psi.text)!!,
    )

    override fun serialize(stub: KotlinConstantExpressionStubImpl, dataStream: StubOutputStream) {
        dataStream.writeVarInt(stub.kind.ordinal)
        dataStream.writeName(stub.value)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinConstantExpressionStubImpl {
        val kindOrdinal = dataStream.readVarInt()
        val value = dataStream.readName() ?: StringRef.fromString("")!!

        return KotlinConstantExpressionStubImpl(
            parent = parentStub,
            kind = ConstantValueKind.entries[kindOrdinal],
            valueRef = value,
        )
    }
}
