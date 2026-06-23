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
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderStubImpl

/**
 * Builds the trivial stub shared by all placeholder element types (no own data). The element type is held only to stamp
 * it into the produced stub and to build PSI; the stubbing policy is computed directly to avoid recursing through the
 * element type, which delegates [KtStubElementType.shouldCreateStub] back to this factory (KT-78356).
 */
@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal open class KotlinPlaceHolderStubFactory<T : KtElementImplStub<out StubElement<*>>>(
    private val elementType: KtStubElementType<KotlinPlaceHolderStubImpl<T>, T>,
) : StubElementFactory<KotlinPlaceHolderStubImpl<T>, T> {
    override fun createStub(psi: T, parentStub: StubElement<out PsiElement>?): KotlinPlaceHolderStubImpl<T> {
        return KotlinPlaceHolderStubImpl(parentStub, elementType)
    }

    @OptIn(KtImplementationDetail::class)
    override fun createPsi(stub: KotlinPlaceHolderStubImpl<T>): T {
        return elementType.createPsiFromStub(stub)
    }

    @OptIn(KtImplementationDetail::class)
    override fun shouldCreateStub(node: ASTNode): Boolean {
        return KtStubElementType.shouldCreateStubDependingOnParent(node)
    }
}

/**
 * Serializes the trivial placeholder stub. The payload is empty; the external id matches the legacy
 * `"kotlin." + debugName` so existing indices stay valid.
 */
internal class KotlinPlaceHolderStubSerializer<T : KtElementImplStub<out StubElement<*>>>(
    private val elementType: KtStubElementType<KotlinPlaceHolderStubImpl<T>, T>,
) : StubSerializer<KotlinPlaceHolderStubImpl<T>> {
    override fun getExternalId(): String = elementType.conventionalExternalId

    override fun serialize(stub: KotlinPlaceHolderStubImpl<T>, dataStream: StubOutputStream) {
        // no data
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinPlaceHolderStubImpl<T> {
        return KotlinPlaceHolderStubImpl(parentStub, elementType)
    }

    override fun indexStub(stub: KotlinPlaceHolderStubImpl<T>, sink: IndexSink) {
        // not indexed
    }
}
