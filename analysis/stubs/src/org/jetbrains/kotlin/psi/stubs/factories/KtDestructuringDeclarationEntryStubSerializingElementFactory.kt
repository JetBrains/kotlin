/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.psiUtil.safeFqNameForLazyResolve
import org.jetbrains.kotlin.psi.stubs.StubUtils.readNullableFqName
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService
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
     */
    override fun shouldCreateStub(node: ASTNode): Boolean {
        val parent = node.treeParent ?: return false
        return parent.elementType == KtNodeTypes.DESTRUCTURING_DECLARATION &&
                KtDestructuringDeclarationStubSerializingElementFactory.shouldCreateStub(parent)
    }

    override fun createStub(
        psi: KtDestructuringDeclarationEntry,
        parentStub: StubElement<*>?,
    ): KotlinDestructuringDeclarationEntryStubImpl = KotlinDestructuringDeclarationEntryStubImpl(
        parent = parentStub,
        name = StringRef.fromString(psi.name),
        fqName = psi.safeFqNameForLazyResolve(),
        isVar = psi.isVar,
        isSingleUnderscore = psi.nameIdentifier?.textMatches("_") == true,
    )

    override fun serialize(stub: KotlinDestructuringDeclarationEntryStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getName())
        dataStream.writeName(stub.fqName?.asString())
        dataStream.writeBoolean(stub.isVar)
        dataStream.writeBoolean(stub.isSingleUnderscore)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinDestructuringDeclarationEntryStubImpl = KotlinDestructuringDeclarationEntryStubImpl(
        parent = parentStub,
        name = dataStream.readName(),
        fqName = dataStream.readNullableFqName(),
        isVar = dataStream.readBoolean(),
        isSingleUnderscore = dataStream.readBoolean(),
    )

    override fun indexStub(stub: KotlinDestructuringDeclarationEntryStubImpl, sink: IndexSink) {
        StubIndexService.getInstance()?.indexDestructuringDeclarationEntry(stub, sink)
    }
}
