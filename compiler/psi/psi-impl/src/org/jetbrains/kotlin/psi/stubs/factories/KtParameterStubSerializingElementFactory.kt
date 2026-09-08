/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.stubs.StubUtils.deserializeKdocText
import org.jetbrains.kotlin.psi.stubs.StubUtils.serializeKdocText
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService
import org.jetbrains.kotlin.psi.stubs.elements.deserializeTypeBean
import org.jetbrains.kotlin.psi.stubs.elements.serializeTypeBean
import org.jetbrains.kotlin.psi.stubs.impl.KotlinParameterStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.deserializeConstantValue
import org.jetbrains.kotlin.psi.stubs.impl.serializeConstantValue

internal object KtParameterStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinParameterStubImpl, KtParameter>(
        type = KtNodeTypes.VALUE_PARAMETER,
    ) {

    override fun createPsi(stub: KotlinParameterStubImpl): KtParameter = KtParameter(stub)

    override fun createStub(
        psi: KtParameter,
        parentStub: StubElement<*>?,
    ): KotlinParameterStubImpl = KotlinParameterStubImpl(
        parent = parentStub,
        fqNameRef = StringRef.fromString(psi.fqName?.asString()),
        name = StringRef.fromString(psi.name),
        isMutable = psi.isMutable,
        hasValOrVar = psi.hasValOrVar(),
        hasDefaultValue = psi.hasDefaultValue(),
        functionTypeParameterName = null,
        equalityBoundType = null,
        kdocText = null,
        constantInitializer = null,
    )

    override fun serialize(stub: KotlinParameterStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getName())
        dataStream.writeBoolean(stub.isMutable)
        dataStream.writeBoolean(stub.hasValOrVar)
        dataStream.writeBoolean(stub.hasDefaultValue)
        dataStream.writeName(stub.fqName?.asString())
        dataStream.writeName(stub.functionTypeParameterName)
        serializeTypeBean(dataStream, stub.equalityBoundType)
        dataStream.serializeKdocText(stub.kdocText)
        serializeConstantValue(stub.constantInitializer, dataStream)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinParameterStubImpl {
        val name = dataStream.readName()
        val isMutable = dataStream.readBoolean()
        val hasValOrVar = dataStream.readBoolean()
        val hasDefaultValue = dataStream.readBoolean()
        val fqName = dataStream.readName()

        return KotlinParameterStubImpl(
            parent = parentStub,
            fqNameRef = fqName,
            name = name,
            isMutable = isMutable,
            hasValOrVar = hasValOrVar,
            hasDefaultValue = hasDefaultValue,
            functionTypeParameterName = dataStream.readNameString(),
            equalityBoundType = deserializeTypeBean(dataStream),
            kdocText = dataStream.deserializeKdocText(),
            constantInitializer = deserializeConstantValue(dataStream),
        )
    }

    override fun indexStub(stub: KotlinParameterStubImpl, sink: IndexSink) {
        StubIndexService.getInstance().indexParameter(stub, sink)
    }
}
