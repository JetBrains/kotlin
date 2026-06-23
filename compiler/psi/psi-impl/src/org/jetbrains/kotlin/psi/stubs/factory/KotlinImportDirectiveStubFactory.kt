/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.impl.KotlinImportDirectiveStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinImportDirectiveStubFactory : StubElementFactory<KotlinImportDirectiveStubImpl, KtImportDirective> {
    @OptIn(KtImplementationDetail::class)
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(psi: KtImportDirective, parentStub: StubElement<out PsiElement>?): KotlinImportDirectiveStubImpl {
        val importedFqName = psi.importedFqName
        val fqName = StringRef.fromString(importedFqName?.asString())
        return KotlinImportDirectiveStubImpl(parentStub, psi.isAllUnder, fqName, psi.isValidImport)
    }

    override fun createPsi(stub: KotlinImportDirectiveStubImpl): KtImportDirective = KtImportDirective(stub)
}

internal object KotlinImportDirectiveStubSerializer : StubSerializer<KotlinImportDirectiveStubImpl> {
    override fun getExternalId(): String = "kotlin.IMPORT_DIRECTIVE"

    override fun serialize(stub: KotlinImportDirectiveStubImpl, dataStream: StubOutputStream) {
        dataStream.writeBoolean(stub.isAllUnder)
        val importedFqName = stub.importedFqName
        dataStream.writeName(importedFqName?.asString())
        dataStream.writeBoolean(stub.isValid)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinImportDirectiveStubImpl {
        val isAllUnder = dataStream.readBoolean()
        val importedName = dataStream.readName()
        val isValid = dataStream.readBoolean()
        return KotlinImportDirectiveStubImpl(parentStub, isAllUnder, importedName, isValid)
    }

    override fun indexStub(stub: KotlinImportDirectiveStubImpl, sink: IndexSink) {
        // not indexed
    }
}
