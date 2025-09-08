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
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames
import org.jetbrains.kotlin.psi.psiUtil.safeFqNameForLazyResolve
import org.jetbrains.kotlin.psi.stubs.KotlinClassStub
import org.jetbrains.kotlin.psi.stubs.StubUtils.createNestedClassId
import org.jetbrains.kotlin.psi.stubs.StubUtils.deserializeClassId
import org.jetbrains.kotlin.psi.stubs.StubUtils.serializeClassId
import org.jetbrains.kotlin.psi.stubs.impl.KotlinClassStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.Utils

internal object KtClassElementType : KtStubElementType<KotlinClassStubImpl, KtClass>(
    /* debugName = */ "CLASS",
    /* psiClass = */ KtClass::class.java,
    /* stubClass = */ KotlinClassStub::class.java,
) {
    override fun createStub(psi: KtClass, parentStub: StubElement<*>): KotlinClassStubImpl {
        val fqName = psi.safeFqNameForLazyResolve()?.asString()
        val classId = createNestedClassId(parentStub, psi)
        val name = psi.getName()
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
            valueClassRepresentation = null,
        )
    }

    override fun serialize(stub: KotlinClassStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getName())
        dataStream.writeName(stub.fqName?.asString())

        serializeClassId(dataStream, stub.classId)

        dataStream.writeBoolean(stub.isInterface)
        dataStream.writeBoolean(stub.isClsStubCompiledToJvmDefaultImplementation)
        dataStream.writeBoolean(stub.isLocal)
        dataStream.writeBoolean(stub.isTopLevel)

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

        val superCount = dataStream.readVarInt()
        val superNames = StringRef.createArray(superCount)
        for (i in 0..<superCount) {
            superNames[i] = dataStream.readName()
        }

        val representationOrdinal = dataStream.readVarInt()
        val representation: KotlinValueClassRepresentation? =
            if (representationOrdinal == 0)
                null
            else
                KotlinValueClassRepresentation.entries[representationOrdinal - 1]

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
            valueClassRepresentation = representation,
        )
    }

    override fun indexStub(stub: KotlinClassStubImpl, sink: IndexSink) {
        StubIndexService.getInstance().indexClass(stub, sink)
    }
}
