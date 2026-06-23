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
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService
import org.jetbrains.kotlin.psi.stubs.impl.KotlinParameterStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinParameterStubFactory : StubElementFactory<KotlinParameterStubImpl, KtParameter> {
    @OptIn(KtImplementationDetail::class)
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(psi: KtParameter, parentStub: StubElement<out PsiElement>?): KotlinParameterStubImpl {
        val fqName = psi.fqName
        val fqNameRef = StringRef.fromString(fqName?.asString())
        return KotlinParameterStubImpl(
            parentStub, fqNameRef, StringRef.fromString(psi.name),
            psi.isMutable, psi.hasValOrVar(), psi.hasDefaultValue(), null,
        )
    }

    override fun createPsi(stub: KotlinParameterStubImpl): KtParameter = KtParameter(stub)
}

internal object KotlinParameterStubSerializer : StubSerializer<KotlinParameterStubImpl> {
    override fun getExternalId(): String = "kotlin.VALUE_PARAMETER"

    override fun serialize(stub: KotlinParameterStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(stub.isMutable)
        dataStream.writeBoolean(stub.hasValOrVar)
        dataStream.writeBoolean(stub.hasDefaultValue)
        val name = stub.fqName
        dataStream.writeName(name?.asString())
        dataStream.writeName(stub.functionTypeParameterName)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinParameterStubImpl {
        val name = dataStream.readName()
        val isMutable = dataStream.readBoolean()
        val hasValOrValNode = dataStream.readBoolean()
        val hasDefaultValue = dataStream.readBoolean()
        val fqName = dataStream.readName()

        return KotlinParameterStubImpl(
            parentStub, fqName, name, isMutable, hasValOrValNode, hasDefaultValue,
            dataStream.readNameString(),
        )
    }

    override fun indexStub(stub: KotlinParameterStubImpl, sink: IndexSink) {
        StubIndexService.getInstance().indexParameter(stub, sink)
    }
}
