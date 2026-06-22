/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import org.jetbrains.kotlin.psi.KtBackingField
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.impl.KotlinBackingFieldStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinBackingFieldStubFactory : StubElementFactory<KotlinBackingFieldStubImpl, KtBackingField> {
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(psi: KtBackingField, parentStub: StubElement<out PsiElement>?): KotlinBackingFieldStubImpl {
        return KotlinBackingFieldStubImpl(parentStub, psi.hasInitializer())
    }

    override fun createPsi(stub: KotlinBackingFieldStubImpl): KtBackingField = KtBackingField(stub)
}

internal object KotlinBackingFieldStubSerializer : StubSerializer<KotlinBackingFieldStubImpl> {
    override fun getExternalId(): String = "kotlin.BACKING_FIELD"

    override fun serialize(stub: KotlinBackingFieldStubImpl, dataStream: StubOutputStream) {
        dataStream.writeBoolean(stub.hasInitializer)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinBackingFieldStubImpl {
        val hasInitializer = dataStream.readBoolean()
        return KotlinBackingFieldStubImpl(parentStub, hasInitializer)
    }

    override fun indexStub(stub: KotlinBackingFieldStubImpl, sink: IndexSink) {
        // not indexed
    }
}
