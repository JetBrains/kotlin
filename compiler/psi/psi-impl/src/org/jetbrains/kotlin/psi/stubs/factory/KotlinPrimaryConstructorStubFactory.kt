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
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.stubs.StubUtils.deserializeKdocText
import org.jetbrains.kotlin.psi.stubs.StubUtils.serializeKdocText
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPrimaryConstructorStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinPrimaryConstructorStubFactory : StubElementFactory<KotlinPrimaryConstructorStubImpl, KtPrimaryConstructor> {
    @OptIn(KtImplementationDetail::class)
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(
        psi: KtPrimaryConstructor,
        parentStub: StubElement<out PsiElement>?,
    ): KotlinPrimaryConstructorStubImpl = KotlinPrimaryConstructorStubImpl(
        parent = parentStub,
        containingClassName = StringRef.fromString(psi.name),
        kdocText = null,
    )

    override fun createPsi(stub: KotlinPrimaryConstructorStubImpl): KtPrimaryConstructor = KtPrimaryConstructor(stub)
}

internal object KotlinPrimaryConstructorStubSerializer : StubSerializer<KotlinPrimaryConstructorStubImpl> {
    override fun getExternalId(): String = "kotlin.PRIMARY_CONSTRUCTOR"

    override fun serialize(stub: KotlinPrimaryConstructorStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.serializeKdocText(stub.kdocText)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinPrimaryConstructorStubImpl {
        val name = dataStream.readName()
        val kdocText = dataStream.deserializeKdocText()
        return KotlinPrimaryConstructorStubImpl(
            parent = parentStub,
            containingClassName = name,
            kdocText = kdocText,
        )
    }

    override fun indexStub(stub: KotlinPrimaryConstructorStubImpl, sink: IndexSink) {
        // not indexed
    }
}
