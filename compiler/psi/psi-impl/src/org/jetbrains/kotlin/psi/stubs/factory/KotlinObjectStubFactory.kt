/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames
import org.jetbrains.kotlin.psi.psiUtil.safeFqNameForLazyResolve
import org.jetbrains.kotlin.psi.stubs.StubUtils
import org.jetbrains.kotlin.psi.stubs.StubUtils.deserializeKdocText
import org.jetbrains.kotlin.psi.stubs.StubUtils.serializeKdocText
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService
import org.jetbrains.kotlin.psi.stubs.impl.KotlinObjectStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.Utils

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinObjectStubFactory : StubElementFactory<KotlinObjectStubImpl, KtObjectDeclaration> {
    /**
     * All objects should have stubs since we want to index even local ones
     */
    override fun shouldCreateStub(node: ASTNode): Boolean = true

    override fun createStub(psi: KtObjectDeclaration, parentStub: StubElement<out PsiElement>?): KotlinObjectStubImpl {
        val name = psi.name
        val fqName = psi.safeFqNameForLazyResolve()
        val superNames = psi.getSuperNames()
        val classId = StubUtils.createClassId(parentStub!!, psi)
        return KotlinObjectStubImpl(
            parentStub,
            StringRef.fromString(name),
            fqName,
            classId,
            Utils.wrapStrings(superNames),
            psi.isTopLevel(),
            psi.isLocal(),
            psi.isObjectLiteral(),
            /* kdocText = */ null,
        )
    }

    override fun createPsi(stub: KotlinObjectStubImpl): KtObjectDeclaration = KtObjectDeclaration(stub)
}

internal object KotlinObjectStubSerializer : StubSerializer<KotlinObjectStubImpl> {
    override fun getExternalId(): String = "kotlin.OBJECT_DECLARATION"

    override fun serialize(stub: KotlinObjectStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)

        val fqName = stub.fqName
        dataStream.writeName(fqName?.toString())

        StubUtils.serializeClassId(dataStream, stub.classId)

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

        val fqNameStr = dataStream.readName()
        val fqName = fqNameStr?.let { FqName(it.toString()) }

        val classId = StubUtils.deserializeClassId(dataStream)

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
            parentStub,
            name,
            fqName,
            classId,
            superNames,
            isTopLevel,
            isLocal,
            isObjectLiteral,
            kdocText,
        )
    }

    override fun indexStub(stub: KotlinObjectStubImpl, sink: IndexSink) {
        StubIndexService.getInstance().indexObject(stub, sink)
    }
}
