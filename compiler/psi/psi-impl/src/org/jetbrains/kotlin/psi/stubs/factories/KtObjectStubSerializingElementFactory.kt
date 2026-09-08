/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames
import org.jetbrains.kotlin.psi.psiUtil.safeFqNameForLazyResolve
import org.jetbrains.kotlin.psi.stubs.StubUtils.createClassId
import org.jetbrains.kotlin.psi.stubs.StubUtils.deserializeClassId
import org.jetbrains.kotlin.psi.stubs.StubUtils.deserializeKdocText
import org.jetbrains.kotlin.psi.stubs.StubUtils.serializeClassId
import org.jetbrains.kotlin.psi.stubs.StubUtils.serializeKdocText
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService
import org.jetbrains.kotlin.psi.stubs.impl.KotlinObjectStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.Utils

internal object KtObjectStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinObjectStubImpl, KtObjectDeclaration>(
        type = KtNodeTypes.OBJECT_DECLARATION,
    ) {

    override fun createPsi(stub: KotlinObjectStubImpl): KtObjectDeclaration = KtObjectDeclaration(stub)

    /**
     * All objects should have stubs since we want to index even local ones
     */
    override fun shouldCreateStub(node: ASTNode): Boolean = true

    override fun createStub(
        psi: KtObjectDeclaration,
        parentStub: StubElement<*>?,
    ): KotlinObjectStubImpl = KotlinObjectStubImpl(
        parent = parentStub,
        name = StringRef.fromString(psi.name),
        fqName = psi.safeFqNameForLazyResolve(),
        classId = parentStub?.let { createClassId(it, psi) },
        superNameRefs = Utils.wrapStrings(psi.getSuperNames()),
        isTopLevel = psi.isTopLevel(),
        isLocal = psi.isLocal(),
        isObjectLiteral = psi.isObjectLiteral(),
        kdocText = null,
    )

    override fun serialize(stub: KotlinObjectStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getName())
        dataStream.writeName(stub.fqName?.asString())

        serializeClassId(dataStream, stub.classId)

        dataStream.writeBoolean(stub.isTopLevel)
        dataStream.writeBoolean(stub.isLocal)
        dataStream.writeBoolean(stub.isObjectLiteral)
        dataStream.serializeKdocText(stub.kdocText)

        val superNames = stub.superNames
        dataStream.writeVarInt(superNames.size)
        for (name in superNames) {
            dataStream.writeName(name)
        }
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinObjectStubImpl {
        val name = dataStream.readName()
        val fqName = dataStream.readName()?.string?.let(::FqName)

        val classId = deserializeClassId(dataStream)

        val isTopLevel = dataStream.readBoolean()
        val isLocal = dataStream.readBoolean()
        val isObjectLiteral = dataStream.readBoolean()
        val kdocText = dataStream.deserializeKdocText()

        val superCount = dataStream.readVarInt()
        val superNames = StringRef.createArray(superCount)
        for (i in 0..<superCount) {
            superNames[i] = dataStream.readName()
        }

        return KotlinObjectStubImpl(
            parent = parentStub,
            name = name,
            fqName = fqName,
            classId = classId,
            superNameRefs = superNames,
            isTopLevel = isTopLevel,
            isLocal = isLocal,
            isObjectLiteral = isObjectLiteral,
            kdocText = kdocText,
        )
    }

    override fun indexStub(stub: KotlinObjectStubImpl, sink: IndexSink) {
        StubIndexService.getInstance().indexObject(stub, sink)
    }
}
