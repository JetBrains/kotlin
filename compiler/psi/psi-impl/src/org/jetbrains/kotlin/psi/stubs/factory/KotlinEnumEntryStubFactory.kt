/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.psiUtil.safeFqNameForLazyResolve
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService
import org.jetbrains.kotlin.psi.stubs.impl.KotlinEnumEntryStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinEnumEntryStubFactory : StubElementFactory<KotlinEnumEntryStubImpl, KtEnumEntry> {
    @OptIn(KtImplementationDetail::class)
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(psi: KtEnumEntry, parentStub: StubElement<out PsiElement>?): KotlinEnumEntryStubImpl {
        val fqName = psi.safeFqNameForLazyResolve()?.asString()
        val name = psi.name
        val isLocal = psi.isLocal()
        return KotlinEnumEntryStubImpl(
            parent = parentStub,
            qualifiedName = StringRef.fromString(fqName),
            name = StringRef.fromString(name),
            isLocal = isLocal,
        )
    }

    override fun createPsi(stub: KotlinEnumEntryStubImpl): KtEnumEntry = KtEnumEntry(stub)
}

internal object KotlinEnumEntryStubSerializer : StubSerializer<KotlinEnumEntryStubImpl> {
    override fun getExternalId(): String = "kotlin.ENUM_ENTRY"

    override fun serialize(stub: KotlinEnumEntryStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.fqName?.asString())
        dataStream.writeName(stub.name)
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
