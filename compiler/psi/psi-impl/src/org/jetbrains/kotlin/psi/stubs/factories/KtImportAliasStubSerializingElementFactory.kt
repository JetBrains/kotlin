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
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.psi.stubs.impl.KotlinImportAliasStubImpl

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
