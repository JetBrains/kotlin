/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.psi.stubs.impl.KotlinConstantExpressionStubImpl

class KtConstantExpressionElementType(@NonNls debugName: String) :
    KtStubElementType<KotlinConstantExpressionStub, KtConstantExpression>(
        debugName,
        KtConstantExpression::class.java,
        KotlinConstantExpressionStub::class.java
    ) {

    override fun shouldCreateStub(node: ASTNode): Boolean {
        val parent = node.treeParent ?: return false
        if (parent.elementType != KtStubElementTypes.VALUE_ARGUMENT) return false

        return super.shouldCreateStub(node)
    }

    override fun createStub(psi: KtConstantExpression, parentStub: StubElement<*>?): KotlinConstantExpressionStub {
        val elementType = psi.node.elementType as? KtConstantExpressionElementType
            ?: throw IllegalStateException("Stub element type is expected for constant")

        val value = psi.text ?: ""

        return KotlinConstantExpressionStubImpl(
            parentStub,
            elementType,
            constantElementTypeToKind(elementType),
            StringRef.fromString(value)
        )
    }

    override fun serialize(stub: KotlinConstantExpressionStub, dataStream: StubOutputStream) {
        dataStream.writeInt(stub.kind().ordinal)
        dataStream.writeName(stub.value())
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinConstantExpressionStub {
        val kindOrdinal = dataStream.readInt()
        val value = dataStream.readName() ?: StringRef.fromString("")

        val valueKind = ConstantValueKind.values()[kindOrdinal]

        return KotlinConstantExpressionStubImpl(
            parentStub,
            kindToConstantElementType(valueKind),
            valueKind,
            value
        )
    }

    companion object {
        fun kindToConstantElementType(kind: ConstantValueKind): KtConstantExpressionElementType {
            return when (kind) {
                ConstantValueKind.NULL -> KtStubElementTypes.NULL
                ConstantValueKind.BOOLEAN_CONSTANT -> KtStubElementTypes.BOOLEAN_CONSTANT
                ConstantValueKind.FLOAT_CONSTANT -> KtStubElementTypes.FLOAT_CONSTANT
                ConstantValueKind.CHARACTER_CONSTANT -> KtStubElementTypes.CHARACTER_CONSTANT
                ConstantValueKind.INTEGER_CONSTANT -> KtStubElementTypes.INTEGER_CONSTANT
            }
        }

        private fun constantElementTypeToKind(elementType: KtConstantExpressionElementType): ConstantValueKind {
            return when (elementType) {
                KtStubElementTypes.NULL -> ConstantValueKind.NULL
                KtStubElementTypes.BOOLEAN_CONSTANT -> ConstantValueKind.BOOLEAN_CONSTANT
                KtStubElementTypes.INTEGER_CONSTANT -> ConstantValueKind.INTEGER_CONSTANT
                KtStubElementTypes.FLOAT_CONSTANT -> ConstantValueKind.FLOAT_CONSTANT
                KtStubElementTypes.CHARACTER_CONSTANT -> ConstantValueKind.CHARACTER_CONSTANT
                else -> throw IllegalStateException("Unknown constant node type: $elementType")
            }
        }
    }
}