/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.elements.deserializeClassTypeBean
import org.jetbrains.kotlin.psi.stubs.elements.deserializeTypeBean
import org.jetbrains.kotlin.psi.stubs.elements.serializeTypeBean
import org.jetbrains.kotlin.psi.stubs.impl.KotlinUserTypeStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinUserTypeStubFactory : StubElementFactory<KotlinUserTypeStubImpl, KtUserType> {
    @OptIn(KtImplementationDetail::class)
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(psi: KtUserType, parentStub: StubElement<out PsiElement>?): KotlinUserTypeStubImpl {
        return KotlinUserTypeStubImpl(parentStub, null, null)
    }

    override fun createPsi(stub: KotlinUserTypeStubImpl): KtUserType = KtUserType(stub)
}

internal object KotlinUserTypeStubSerializer : StubSerializer<KotlinUserTypeStubImpl> {
    override fun getExternalId(): String = "kotlin.USER_TYPE"

    override fun serialize(stub: KotlinUserTypeStubImpl, dataStream: StubOutputStream) {
        serializeTypeBean(dataStream, stub.upperBound)
        serializeTypeBean(dataStream, stub.abbreviatedType)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinUserTypeStubImpl {
        return KotlinUserTypeStubImpl(
            parentStub,
            deserializeTypeBean(dataStream),
            deserializeClassTypeBean(dataStream),
        )
    }

    override fun indexStub(stub: KotlinUserTypeStubImpl, sink: IndexSink) {
        // not indexed
    }
}
