/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.kotlin.contracts.description.ExpressionType
import org.jetbrains.kotlin.psi.KtContractExpression
import org.jetbrains.kotlin.psi.stubs.KotlinContractExpressionStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinContractExpressionStubImpl

class KtContractExpressionElementType(debugName: String) : KtStubElementType<KotlinContractExpressionStub, KtContractExpression>(
    debugName,
    KtContractExpression::class.java,
    KotlinContractExpressionStub::class.java
) {
    override fun createStub(
        psi: KtContractExpression,
        parentStub: StubElement<*>?
    ): KotlinContractExpressionStub {
        return KotlinContractExpressionStubImpl(parentStub, ExpressionType.NONE, "")
    }

    override fun serialize(
        stub: KotlinContractExpressionStub,
        dataStream: StubOutputStream
    ) {
        dataStream.writeName(stub.type().name)
        dataStream.writeName(stub.data())
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?
    ): KotlinContractExpressionStub {
        return KotlinContractExpressionStubImpl(
            parentStub,
            ExpressionType.valueOf(dataStream.readNameString() ?: "NONE"),
            dataStream.readNameString() ?: ""
        )
    }
}