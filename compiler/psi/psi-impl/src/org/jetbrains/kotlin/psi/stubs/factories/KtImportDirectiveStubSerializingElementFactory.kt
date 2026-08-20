/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.stubs.impl.KotlinImportDirectiveStubImpl

internal object KtImportDirectiveStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinImportDirectiveStubImpl, KtImportDirective>(
        type = KtNodeTypes.IMPORT_DIRECTIVE,
    ) {

    override fun createPsi(stub: KotlinImportDirectiveStubImpl): KtImportDirective = KtImportDirective(stub)

    override fun createStub(
        psi: KtImportDirective,
        parentStub: StubElement<*>?,
    ): KotlinImportDirectiveStubImpl = KotlinImportDirectiveStubImpl(
        parent = parentStub,
        isAllUnder = psi.isAllUnder,
        importedFqNameRef = StringRef.fromString(psi.importedFqName?.asString()),
        isValid = psi.isValidImport,
    )

    override fun serialize(stub: KotlinImportDirectiveStubImpl, dataStream: StubOutputStream) {
        dataStream.writeBoolean(stub.isAllUnder)
        dataStream.writeName(stub.importedFqName?.asString())
        dataStream.writeBoolean(stub.isValid)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinImportDirectiveStubImpl = KotlinImportDirectiveStubImpl(
        parent = parentStub,
        isAllUnder = dataStream.readBoolean(),
        importedFqNameRef = dataStream.readName(),
        isValid = dataStream.readBoolean(),
    )
}
