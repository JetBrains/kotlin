/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.elements.deserializeClassTypeBean
import org.jetbrains.kotlin.psi.stubs.elements.serializeTypeBean
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFunctionTypeStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinFunctionTypeStubFactory : StubElementFactory<KotlinFunctionTypeStubImpl, KtFunctionType> {
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(psi: KtFunctionType, parentStub: StubElement<out PsiElement>?): KotlinFunctionTypeStubImpl =
        KotlinFunctionTypeStubImpl(
            parent = parentStub,
            abbreviatedType = null,
        )

    override fun createPsi(stub: KotlinFunctionTypeStubImpl): KtFunctionType = KtFunctionType(stub)
}

internal object KotlinFunctionTypeStubSerializer : StubSerializer<KotlinFunctionTypeStubImpl> {
    override fun getExternalId(): String = "kotlin.FUNCTION_TYPE"

    override fun serialize(stub: KotlinFunctionTypeStubImpl, dataStream: StubOutputStream) {
        serializeTypeBean(dataStream, stub.abbreviatedType)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinFunctionTypeStubImpl {
        val abbreviatedType = deserializeClassTypeBean(dataStream)
        return KotlinFunctionTypeStubImpl(
            parent = parentStub,
            abbreviatedType = abbreviatedType,
        )
    }

    override fun indexStub(stub: KotlinFunctionTypeStubImpl, sink: IndexSink) {
        // not indexed
    }
}
