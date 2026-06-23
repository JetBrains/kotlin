/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtTypeProjection
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeProjectionStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinTypeProjectionStubFactory : StubElementFactory<KotlinTypeProjectionStubImpl, KtTypeProjection> {
    @OptIn(KtImplementationDetail::class)
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(psi: KtTypeProjection, parentStub: StubElement<out PsiElement>?): KotlinTypeProjectionStubImpl {
        return KotlinTypeProjectionStubImpl(parentStub, psi.projectionKind.ordinal)
    }

    override fun createPsi(stub: KotlinTypeProjectionStubImpl): KtTypeProjection = KtTypeProjection(stub)
}

internal object KotlinTypeProjectionStubSerializer : StubSerializer<KotlinTypeProjectionStubImpl> {
    override fun getExternalId(): String = "kotlin.TYPE_PROJECTION"

    override fun serialize(stub: KotlinTypeProjectionStubImpl, dataStream: StubOutputStream) {
        dataStream.writeVarInt(stub.projectionKind.ordinal)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinTypeProjectionStubImpl {
        val projectionKindOrdinal = dataStream.readVarInt()
        return KotlinTypeProjectionStubImpl(parentStub, projectionKindOrdinal)
    }

    override fun indexStub(stub: KotlinTypeProjectionStubImpl, sink: IndexSink) {
        // not indexed
    }
}
