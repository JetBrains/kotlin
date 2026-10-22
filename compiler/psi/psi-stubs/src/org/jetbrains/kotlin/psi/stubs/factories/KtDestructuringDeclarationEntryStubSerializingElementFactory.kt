/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.stubs.impl.KotlinDestructuringDeclarationEntryStubImpl

internal object KtDestructuringDeclarationEntryStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinDestructuringDeclarationEntryStubImpl, KtDestructuringDeclarationEntry>(
        type = KtNodeTypes.DESTRUCTURING_DECLARATION_ENTRY,
    ) {

    override fun createPsi(
        stub: KotlinDestructuringDeclarationEntryStubImpl,
    ): KtDestructuringDeclarationEntry = KtDestructuringDeclarationEntry(stub)

    /**
     * An entry is stubbed only if its destructuring declaration is stubbed.
     *
     * The entries of top-level and member destructuring declarations are wrapped in an error element by the parser (KT-58563).
     */
    override fun shouldCreateStub(node: ASTNode): Boolean {
        var parent = node.treeParent ?: return false
        if (parent.elementType == TokenType.ERROR_ELEMENT) {
            parent = parent.treeParent ?: return false
        }

        return parent.elementType == KtNodeTypes.DESTRUCTURING_DECLARATION &&
                KtDestructuringDeclarationStubSerializingElementFactory.shouldCreateStub(parent)
    }

    override fun createStub(
        psi: KtDestructuringDeclarationEntry,
        parentStub: StubElement<*>?,
    ): KotlinDestructuringDeclarationEntryStubImpl = KotlinDestructuringDeclarationEntryStubImpl(
        parent = parentStub,
        name = StringRef.fromString(psi.name),
        isVar = psi.isVar,
        isSingleUnderscore = psi.nameIdentifier?.textMatches("_") == true,
    )

    override fun serialize(stub: KotlinDestructuringDeclarationEntryStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getName())
        dataStream.writeBoolean(stub.isVar)
        dataStream.writeBoolean(stub.isSingleUnderscore)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinDestructuringDeclarationEntryStubImpl = KotlinDestructuringDeclarationEntryStubImpl(
        parent = parentStub,
        name = dataStream.readName(),
        isVar = dataStream.readBoolean(),
        isSingleUnderscore = dataStream.readBoolean(),
    )
}
