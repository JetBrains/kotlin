/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtValueArgumentElementType
import org.jetbrains.kotlin.psi.stubs.impl.KotlinValueArgumentStubImpl

/**
 * Builds the value-argument / lambda-argument stub, whose only payload is the spread flag (KT-78356).
 */
@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal class KotlinValueArgumentStubFactory<T : KtValueArgument>(
    private val elementType: KtValueArgumentElementType<T>,
) : StubElementFactory<KotlinValueArgumentStubImpl<T>, T> {
    override fun createStub(psi: T, parentStub: StubElement<out PsiElement>?): KotlinValueArgumentStubImpl<T> {
        return KotlinValueArgumentStubImpl(parentStub, elementType, psi.isSpread)
    }

    override fun createPsi(stub: KotlinValueArgumentStubImpl<T>): T {
        @OptIn(KtImplementationDetail::class)
        return elementType.createPsiFromStub(stub)
    }

    @OptIn(KtImplementationDetail::class)
    override fun shouldCreateStub(node: ASTNode): Boolean {
        return KtStubElementType.shouldCreateStubDependingOnParent(node)
    }
}

internal class KotlinValueArgumentStubSerializer<T : KtValueArgument>(
    private val elementType: KtValueArgumentElementType<T>,
) : StubSerializer<KotlinValueArgumentStubImpl<T>> {
    override fun getExternalId(): String = elementType.conventionalExternalId

    override fun serialize(stub: KotlinValueArgumentStubImpl<T>, dataStream: StubOutputStream) {
        dataStream.writeBoolean(stub.isSpread)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinValueArgumentStubImpl<T> {
        val isSpread = dataStream.readBoolean()
        return KotlinValueArgumentStubImpl(parentStub, elementType, isSpread)
    }

    override fun indexStub(stub: KotlinValueArgumentStubImpl<T>, sink: IndexSink) {
        // not indexed
    }
}
