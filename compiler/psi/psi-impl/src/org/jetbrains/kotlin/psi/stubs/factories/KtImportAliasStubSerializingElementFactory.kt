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
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.psi.stubs.impl.KotlinImportAliasStubImpl
import org.jetbrains.kotlin.psi.stubs.light.nameIdentifier
import org.jetbrains.kotlin.psi.stubs.light.nodeText

internal object KtImportAliasStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinImportAliasStubImpl, KtImportAlias>(
        type = KtNodeTypes.IMPORT_ALIAS,
    ) {

    override fun createPsi(stub: KotlinImportAliasStubImpl): KtImportAlias = KtImportAlias(stub)

    override fun createStub(
        psi: KtImportAlias,
        parentStub: StubElement<*>?,
    ): KotlinImportAliasStubImpl = KotlinImportAliasStubImpl(
        parent = parentStub,
        name = StringRef.fromString(psi.name),
    )

    override fun createStub(
        tree: LighterAST,
        node: LighterASTNode,
        parentStub: StubElement<*>,
    ): KotlinImportAliasStubImpl = KotlinImportAliasStubImpl(
        parent = parentStub,
        // Mirrors `KtImportAlias.getName`, which does not unquote the identifier
        name = StringRef.fromString(tree.nameIdentifier(node)?.let(tree::nodeText)),
    )

    override fun serialize(stub: KotlinImportAliasStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinImportAliasStubImpl = KotlinImportAliasStubImpl(
        parent = parentStub,
        name = dataStream.readName(),
    )
}
