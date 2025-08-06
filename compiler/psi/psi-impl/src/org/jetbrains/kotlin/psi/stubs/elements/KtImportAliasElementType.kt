/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.psi.stubs.KotlinImportAliasStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinImportAliasStubImpl

class KtImportAliasElementType(debugName: String) :
    KtStubElementType<KotlinImportAliasStubImpl, KtImportAlias>(
        debugName,
        KtImportAlias::class.java,
        KotlinImportAliasStub::class.java,
    ) {

    override fun createStub(psi: KtImportAlias, parentStub: StubElement<*>?): KotlinImportAliasStubImpl {
        return KotlinImportAliasStubImpl(parentStub, StringRef.fromString(psi.name))
    }

    override fun serialize(stub: KotlinImportAliasStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinImportAliasStubImpl {
        val name = dataStream.readName()
        return KotlinImportAliasStubImpl(parentStub, name)
    }
}
