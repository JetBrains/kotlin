/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.stubs.KotlinAnnotationEntryStub
import org.jetbrains.kotlin.psi.stubs.StubUtils.readNullableMap
import org.jetbrains.kotlin.psi.stubs.StubUtils.writeNullableMap
import org.jetbrains.kotlin.psi.stubs.impl.KotlinAnnotationEntryStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.deserializeConstantValue
import org.jetbrains.kotlin.psi.stubs.impl.serializeConstantValue

internal object KtAnnotationEntryElementType : KtStubElementType<KotlinAnnotationEntryStubImpl, KtAnnotationEntry>(
    "ANNOTATION_ENTRY",
    KtAnnotationEntry::class.java,
    KotlinAnnotationEntryStub::class.java,
) {
    override fun createStub(psi: KtAnnotationEntry, parentStub: StubElement<*>?): KotlinAnnotationEntryStubImpl {
        val shortName = psi.getShortName()
        val resultName = shortName?.asString()
        val valueArgumentList = psi.getValueArgumentList()
        val hasValueArguments = valueArgumentList != null && !valueArgumentList.arguments.isEmpty()
        return KotlinAnnotationEntryStubImpl(
            parent = parentStub,
            shortNameRef = StringRef.fromString(resultName),
            hasValueArguments = hasValueArguments,
            valueArguments = null,
        )
    }

    override fun serialize(stub: KotlinAnnotationEntryStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.shortName)
        dataStream.writeBoolean(stub.hasValueArguments)
        dataStream.writeNullableMap(
            map = stub.valueArguments,
            keyWriter = { writeName(it.asString()) },
            valueWriter = { serializeConstantValue(it, this) },
        )
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinAnnotationEntryStubImpl {
        val shortNameRef = dataStream.readName()
        val hasValueArguments = dataStream.readBoolean()
        val valueArguments = dataStream.readNullableMap(
            keyReader = { Name.identifier(dataStream.readNameString()!!) },
            valueReader = { deserializeConstantValue(this)!! },
        )

        return KotlinAnnotationEntryStubImpl(
            parentStub,
            shortNameRef,
            hasValueArguments,
            valueArguments,
        )
    }

    override fun indexStub(stub: KotlinAnnotationEntryStubImpl, sink: IndexSink) {
        StubIndexService.getInstance().indexAnnotation(stub, sink)
    }
}
