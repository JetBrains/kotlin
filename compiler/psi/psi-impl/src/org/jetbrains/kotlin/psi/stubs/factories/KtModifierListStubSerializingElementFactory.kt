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
import com.intellij.util.io.DataInputOutputUtil
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtDeclarationModifierList
import org.jetbrains.kotlin.psi.stubs.impl.KotlinModifierListStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.ModifierMaskUtils

internal object KtModifierListStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinModifierListStubImpl, KtDeclarationModifierList>(
        type = KtNodeTypes.MODIFIER_LIST,
    ) {

    override fun createPsi(
        stub: KotlinModifierListStubImpl,
    ): KtDeclarationModifierList = KtDeclarationModifierList(stub)

    override fun createStub(
        psi: KtDeclarationModifierList,
        parentStub: StubElement<*>?,
    ): KotlinModifierListStubImpl = KotlinModifierListStubImpl(
        parent = parentStub,
        mask = ModifierMaskUtils.computeMaskFromModifierList(psi),
    )

    override fun createStub(
        tree: LighterAST,
        node: LighterASTNode,
        parentStub: StubElement<*>,
    ): KotlinModifierListStubImpl = KotlinModifierListStubImpl(
        parent = parentStub,
        mask = modifierMask(tree, node),
    )

    /**
     * Mirrors [ModifierMaskUtils.computeMaskFromModifierList]. The children are read once, as a light tree rebuilds them on every
     * access, while the mask checks every modifier keyword.
     */
    private fun modifierMask(tree: LighterAST, node: LighterASTNode): Long {
        val childTypes = tree.getChildren(node).map { it.tokenType }
        return ModifierMaskUtils.computeMask { it in childTypes }
    }

    override fun serialize(stub: KotlinModifierListStubImpl, dataStream: StubOutputStream) {
        DataInputOutputUtil.writeLONG(dataStream, stub.mask)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinModifierListStubImpl = KotlinModifierListStubImpl(
        parent = parentStub,
        mask = DataInputOutputUtil.readLONG(dataStream),
    )
}
