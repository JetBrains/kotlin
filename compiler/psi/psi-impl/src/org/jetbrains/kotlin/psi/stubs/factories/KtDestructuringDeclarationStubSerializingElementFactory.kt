/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.impl.KotlinDestructuringDeclarationStubImpl

internal object KtDestructuringDeclarationStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinDestructuringDeclarationStubImpl, KtDestructuringDeclaration>(
        type = KtNodeTypes.DESTRUCTURING_DECLARATION,
    ) {

    override fun createPsi(
        stub: KotlinDestructuringDeclarationStubImpl,
    ): KtDestructuringDeclaration = KtDestructuringDeclaration(stub)

    override fun shouldCreateStub(node: ASTNode): Boolean {
        val parent = node.treeParent
        return when (parent?.elementType) {
            KtNodeTypes.FILE, KtNodeTypes.CLASS_BODY -> true
            KtNodeTypes.BLOCK -> parent.treeParent?.elementType == KtNodeTypes.SCRIPT
            else -> false
        }
    }

    override fun createStub(
        psi: KtDestructuringDeclaration,
        parentStub: StubElement<*>?,
    ): KotlinDestructuringDeclarationStubImpl = KotlinDestructuringDeclarationStubImpl(
        parent = parentStub,
        isVar = psi.isVar,
        hasInitializer = psi.hasInitializer(),
    )

    override fun serialize(stub: KotlinDestructuringDeclarationStubImpl, dataStream: StubOutputStream) {
        dataStream.writeBoolean(stub.isVar)
        dataStream.writeBoolean(stub.hasInitializer)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinDestructuringDeclarationStubImpl = KotlinDestructuringDeclarationStubImpl(
        parent = parentStub,
        isVar = dataStream.readBoolean(),
        hasInitializer = dataStream.readBoolean(),
    )
}
