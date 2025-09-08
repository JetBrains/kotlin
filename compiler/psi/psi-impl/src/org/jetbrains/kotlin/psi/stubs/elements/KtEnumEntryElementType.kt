/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.psiUtil.safeFqNameForLazyResolve
import org.jetbrains.kotlin.psi.stubs.KotlinClassStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinEnumEntryStubImpl

internal object KtEnumEntryElementType : KtStubElementType<KotlinEnumEntryStubImpl, KtEnumEntry>(
    "ENUM_ENTRY",
    KtEnumEntry::class.java,
    KotlinClassStub::class.java,
) {
    override fun createStub(psi: KtEnumEntry, parentStub: StubElement<*>): KotlinEnumEntryStubImpl {
        val fqName = psi.safeFqNameForLazyResolve()?.asString()
        val name = psi.getName()
        val isLocal = psi.isLocal()
        return KotlinEnumEntryStubImpl(
            parent = parentStub,
            qualifiedName = StringRef.fromString(fqName),
            name = StringRef.fromString(name),
            isLocal = isLocal,
        )
    }

    override fun serialize(stub: KotlinEnumEntryStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.fqName?.asString())
        dataStream.writeName(stub.getName())
        dataStream.writeBoolean(stub.isLocal)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinEnumEntryStubImpl {
        val qualifiedName = dataStream.readName()
        val name = dataStream.readName()
        val isLocal = dataStream.readBoolean()

        return KotlinEnumEntryStubImpl(
            parent = parentStub,
            qualifiedName = qualifiedName,
            name = name,
            isLocal = isLocal,
        )
    }

    override fun indexStub(stub: KotlinEnumEntryStubImpl, sink: IndexSink) {
        StubIndexService.getInstance().indexClass(stub, sink)
    }
}
