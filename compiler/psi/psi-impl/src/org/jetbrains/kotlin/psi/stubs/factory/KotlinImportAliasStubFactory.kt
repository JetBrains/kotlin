/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.impl.KotlinImportAliasStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinImportAliasStubFactory : StubElementFactory<KotlinImportAliasStubImpl, KtImportAlias> {
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(psi: KtImportAlias, parentStub: StubElement<out PsiElement>?): KotlinImportAliasStubImpl {
        return KotlinImportAliasStubImpl(parentStub, StringRef.fromString(psi.name))
    }

    override fun createPsi(stub: KotlinImportAliasStubImpl): KtImportAlias = KtImportAlias(stub)
}

internal object KotlinImportAliasStubSerializer : StubSerializer<KotlinImportAliasStubImpl> {
    override fun getExternalId(): String = "kotlin.IMPORT_ALIAS"

    override fun serialize(stub: KotlinImportAliasStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinImportAliasStubImpl {
        val name = dataStream.readName()
        return KotlinImportAliasStubImpl(parentStub, name)
    }

    override fun indexStub(stub: KotlinImportAliasStubImpl, sink: IndexSink) {
        // not indexed
    }
}
