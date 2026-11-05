/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.lang.LighterAST
import com.intellij.lang.LighterASTNode
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtProjectionKind
import org.jetbrains.kotlin.psi.KtTypeProjection
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeProjectionStubImpl
import org.jetbrains.kotlin.psi.stubs.light.firstChildOfType
import org.jetbrains.kotlin.psi.stubs.light.modifierList

internal object KtTypeProjectionStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinTypeProjectionStubImpl, KtTypeProjection>(
        type = KtNodeTypes.TYPE_PROJECTION,
    ) {

    override fun createPsi(stub: KotlinTypeProjectionStubImpl): KtTypeProjection = KtTypeProjection(stub)

    override fun createStub(
        psi: KtTypeProjection,
        parentStub: StubElement<*>?,
    ): KotlinTypeProjectionStubImpl = KotlinTypeProjectionStubImpl(
        parent = parentStub,
        projectionKindOrdinal = psi.projectionKind.ordinal,
    )

    override fun createStub(
        tree: LighterAST,
        node: LighterASTNode,
        parentStub: StubElement<*>,
    ): KotlinTypeProjectionStubImpl = KotlinTypeProjectionStubImpl(
        parent = parentStub,
        projectionKindOrdinal = projectionKind(tree, node).ordinal,
    )

    /** Mirrors `KtTypeProjection.getProjectionKind` and `getProjectionToken`. */
    private fun projectionKind(tree: LighterAST, node: LighterASTNode): KtProjectionKind {
        if (tree.firstChildOfType(node, KtTokens.MUL) != null) return KtProjectionKind.STAR

        val modifierList = tree.modifierList(node)
        if (modifierList != null) {
            if (tree.firstChildOfType(modifierList, KtTokens.IN_KEYWORD) != null) return KtProjectionKind.IN
            if (tree.firstChildOfType(modifierList, KtTokens.OUT_KEYWORD) != null) return KtProjectionKind.OUT
        }

        return KtProjectionKind.NONE
    }

    override fun serialize(stub: KotlinTypeProjectionStubImpl, dataStream: StubOutputStream) {
        dataStream.writeVarInt(stub.projectionKind.ordinal)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinTypeProjectionStubImpl = KotlinTypeProjectionStubImpl(
        parent = parentStub,
        projectionKindOrdinal = dataStream.readVarInt(),
    )
}
