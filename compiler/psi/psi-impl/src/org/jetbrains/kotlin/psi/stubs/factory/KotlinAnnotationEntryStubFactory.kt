/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.stubs.StubUtils.readNullableMap
import org.jetbrains.kotlin.psi.stubs.StubUtils.writeNullableMap
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService
import org.jetbrains.kotlin.psi.stubs.impl.KotlinAnnotationEntryStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.deserializeConstantValue
import org.jetbrains.kotlin.psi.stubs.impl.serializeConstantValue

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinAnnotationEntryStubFactory : StubElementFactory<KotlinAnnotationEntryStubImpl, KtAnnotationEntry> {
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(psi: KtAnnotationEntry, parentStub: StubElement<out PsiElement>?): KotlinAnnotationEntryStubImpl {
        val shortName = psi.getShortName()
        val resultName = shortName?.asString()
        val valueArgumentList = psi.valueArgumentList
        val hasValueArguments = valueArgumentList != null && valueArgumentList.arguments.isNotEmpty()
        return KotlinAnnotationEntryStubImpl(
            parent = parentStub,
            shortNameRef = StringRef.fromString(resultName),
            hasValueArguments = hasValueArguments,
            valueArguments = null,
        )
    }

    override fun createPsi(stub: KotlinAnnotationEntryStubImpl): KtAnnotationEntry = KtAnnotationEntry(stub)
}

internal object KotlinAnnotationEntryStubSerializer : StubSerializer<KotlinAnnotationEntryStubImpl> {
    override fun getExternalId(): String = "kotlin.ANNOTATION_ENTRY"

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
