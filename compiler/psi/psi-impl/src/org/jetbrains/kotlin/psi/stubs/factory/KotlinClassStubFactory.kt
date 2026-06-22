/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames
import org.jetbrains.kotlin.psi.psiUtil.safeFqNameForLazyResolve
import org.jetbrains.kotlin.psi.stubs.StubUtils.createClassId
import org.jetbrains.kotlin.psi.stubs.StubUtils.deserializeClassId
import org.jetbrains.kotlin.psi.stubs.StubUtils.deserializeKdocText
import org.jetbrains.kotlin.psi.stubs.StubUtils.serializeClassId
import org.jetbrains.kotlin.psi.stubs.StubUtils.serializeKdocText
import org.jetbrains.kotlin.psi.stubs.elements.KotlinValueClassRepresentation
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService
import org.jetbrains.kotlin.psi.stubs.impl.KotlinClassStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.Utils

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinClassStubFactory : StubElementFactory<KotlinClassStubImpl, KtClass> {
    /**
     * All classes should have stubs since we want to index even local ones
     */
    override fun shouldCreateStub(node: ASTNode): Boolean = true

    override fun createStub(psi: KtClass, parentStub: StubElement<out PsiElement>?): KotlinClassStubImpl {
        val fqName = psi.safeFqNameForLazyResolve()?.asString()
        val classId = createClassId(parentStub!!, psi)
        val name = psi.name
        val superNames = psi.getSuperNames()
        val isInterface = psi.isInterface()
        val isLocal = psi.isLocal()
        val isTopLevel = psi.isTopLevel()
        return KotlinClassStubImpl(
            parent = parentStub,
            qualifiedName = StringRef.fromString(fqName),
            classId = classId,
            name = StringRef.fromString(name),
            superNameRefs = Utils.wrapStrings(superNames),
            isInterface = isInterface,
            isClsStubCompiledToJvmDefaultImplementation = false,
            isLocal = isLocal,
            isTopLevel = isTopLevel,
            kdocText = null,
            valueClassRepresentation = null,
        )
    }

    override fun createPsi(stub: KotlinClassStubImpl): KtClass = KtClass(stub)
}

internal object KotlinClassStubSerializer : StubSerializer<KotlinClassStubImpl> {
    override fun getExternalId(): String = "kotlin.CLASS"

    override fun serialize(stub: KotlinClassStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeName(stub.fqName?.asString())

        serializeClassId(dataStream, stub.classId)

        dataStream.writeBoolean(stub.isInterface)
        dataStream.writeBoolean(stub.isClsStubCompiledToJvmDefaultImplementation)
        dataStream.writeBoolean(stub.isLocal)
        dataStream.writeBoolean(stub.isTopLevel)
        dataStream.serializeKdocText(stub.kdocText)

        val superNames = stub.superNames
        dataStream.writeVarInt(superNames.size)
        for (name in superNames) {
            dataStream.writeName(name)
        }

        val representation = stub.valueClassRepresentation
        dataStream.writeVarInt(if (representation == null) 0 else representation.ordinal + 1)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinClassStubImpl {
        val name = dataStream.readName()
        val qualifiedName = dataStream.readName()

        val classId = deserializeClassId(dataStream)

        val isInterface = dataStream.readBoolean()
        val isClsStubCompiledToJvmDefaultImplementation = dataStream.readBoolean()
        val isLocal = dataStream.readBoolean()
        val isTopLevel = dataStream.readBoolean()
        val kdocText = dataStream.deserializeKdocText()

        val superCount = dataStream.readVarInt()
        val superNames = StringRef.createArray(superCount)
        for (i in 0..<superCount) {
            superNames[i] = dataStream.readName()
        }

        val representationOrdinal = dataStream.readVarInt()
        val representation: KotlinValueClassRepresentation? =
            if (representationOrdinal == 0) null
            else KotlinValueClassRepresentation.entries[representationOrdinal - 1]

        return KotlinClassStubImpl(
            parent = parentStub,
            qualifiedName = qualifiedName,
            classId = classId,
            name = name,
            superNameRefs = superNames,
            isInterface = isInterface,
            isClsStubCompiledToJvmDefaultImplementation = isClsStubCompiledToJvmDefaultImplementation,
            isLocal = isLocal,
            isTopLevel = isTopLevel,
            kdocText = kdocText,
            valueClassRepresentation = representation,
        )
    }

    override fun indexStub(stub: KotlinClassStubImpl, sink: IndexSink) {
        StubIndexService.getInstance().indexClass(stub, sink)
    }
}
