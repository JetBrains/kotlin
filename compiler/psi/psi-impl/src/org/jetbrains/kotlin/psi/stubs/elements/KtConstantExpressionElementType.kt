/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.stubs.ConstantValueKind
import org.jetbrains.kotlin.psi.stubs.KotlinConstantExpressionStub
import org.jetbrains.kotlin.psi.stubs.StubUtils
import org.jetbrains.kotlin.psi.stubs.impl.KotlinConstantExpressionStubImpl
import org.jetbrains.kotlin.psi.utils.toConstantExpressionElementType
import org.jetbrains.kotlin.psi.utils.toConstantValueKind

class KtConstantExpressionElementType(@NonNls debugName: String) :
    KtStubElementType<KotlinConstantExpressionStubImpl, KtConstantExpression>(
        debugName,
        KtConstantExpression::class.java,
        KotlinConstantExpressionStub::class.java,
    ) {

    override fun shouldCreateStub(node: ASTNode): Boolean {
        if (!StubUtils.isDeclaredInsideValueArgument(node)) {
            return false
        }

        return super.shouldCreateStub(node)
    }

    override fun createStub(psi: KtConstantExpression, parentStub: StubElement<*>?): KotlinConstantExpressionStubImpl {
        val elementType = psi.node.elementType as? KtConstantExpressionElementType
            ?: throw IllegalStateException("Stub element type is expected for constant")

        val value = psi.text

        return KotlinConstantExpressionStubImpl(
            parentStub,
            elementType,
            elementType.toConstantValueKind(),
            StringRef.fromString(value)
        )
    }

    override fun serialize(stub: KotlinConstantExpressionStubImpl, dataStream: StubOutputStream) {
        dataStream.writeVarInt(stub.kind().ordinal)
        dataStream.writeName(stub.value())
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinConstantExpressionStubImpl {
        val kindOrdinal = dataStream.readVarInt()
        val value = dataStream.readName() ?: StringRef.fromString("")

        val valueKind = ConstantValueKind.entries[kindOrdinal]

        return KotlinConstantExpressionStubImpl(
            parentStub,
            valueKind.toConstantExpressionElementType() as KtConstantExpressionElementType,
            valueKind,
            value
        )
    }

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