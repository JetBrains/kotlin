/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import org.jetbrains.kotlin.psi.KtElementImplStub
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderWithTextStubImpl

/**
 * Builds the placeholder-with-text stub (short/literal/escape string-template entries), whose payload is the element
 * text (KT-78356).
 */
@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal class KotlinPlaceHolderWithTextStubFactory<T : KtElementImplStub<*>>(
    private val elementType: KtStubElementType<KotlinPlaceHolderWithTextStubImpl<T>, T>,
) : StubElementFactory<KotlinPlaceHolderWithTextStubImpl<T>, T> {
    override fun createStub(psi: T, parentStub: StubElement<out PsiElement>?): KotlinPlaceHolderWithTextStubImpl<T> {
        return KotlinPlaceHolderWithTextStubImpl(parentStub, elementType, psi.text)
    }

    override fun createPsi(stub: KotlinPlaceHolderWithTextStubImpl<T>): T {
        @OptIn(KtImplementationDetail::class)
        return elementType.createPsiFromStub(stub)
    }

    @OptIn(KtImplementationDetail::class)
    override fun shouldCreateStub(node: ASTNode): Boolean {
        return KtStubElementType.shouldCreateStubDependingOnParent(node)
    }
}

internal class KotlinPlaceHolderWithTextStubSerializer<T : KtElementImplStub<*>>(
    private val elementType: KtStubElementType<KotlinPlaceHolderWithTextStubImpl<T>, T>,
) : StubSerializer<KotlinPlaceHolderWithTextStubImpl<T>> {
    override fun getExternalId(): String = elementType.conventionalExternalId

    override fun serialize(stub: KotlinPlaceHolderWithTextStubImpl<T>, dataStream: StubOutputStream) {
        dataStream.writeUTFFast(stub.text)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinPlaceHolderWithTextStubImpl<T> {
        val text = dataStream.readUTFFast()
        return KotlinPlaceHolderWithTextStubImpl(parentStub, elementType, text)
    }

    override fun indexStub(stub: KotlinPlaceHolderWithTextStubImpl<T>, sink: IndexSink) {
        // not indexed
    }
}
