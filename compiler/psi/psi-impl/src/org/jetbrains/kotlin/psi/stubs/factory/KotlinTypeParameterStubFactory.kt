/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeParameterStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinTypeParameterStubFactory : StubElementFactory<KotlinTypeParameterStubImpl, KtTypeParameter> {
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(psi: KtTypeParameter, parentStub: StubElement<out PsiElement>?): KotlinTypeParameterStubImpl {
        return KotlinTypeParameterStubImpl(parentStub, StringRef.fromString(psi.name))
    }

    override fun createPsi(stub: KotlinTypeParameterStubImpl): KtTypeParameter = KtTypeParameter(stub)
}

internal object KotlinTypeParameterStubSerializer : StubSerializer<KotlinTypeParameterStubImpl> {
    override fun getExternalId(): String = "kotlin.TYPE_PARAMETER"

    override fun serialize(stub: KotlinTypeParameterStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinTypeParameterStubImpl {
        val name = dataStream.readName()
        return KotlinTypeParameterStubImpl(parentStub, name)
    }

    override fun indexStub(stub: KotlinTypeParameterStubImpl, sink: IndexSink) {
        // not indexed
    }
}
