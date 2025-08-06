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
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.psiUtil.safeFqNameForLazyResolve
import org.jetbrains.kotlin.psi.stubs.KotlinTypeAliasStub
import org.jetbrains.kotlin.psi.stubs.StubUtils
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeAliasStubImpl

class KtTypeAliasElementType(debugName: String) :
    KtStubElementType<KotlinTypeAliasStubImpl, KtTypeAlias>(debugName, KtTypeAlias::class.java, KotlinTypeAliasStub::class.java) {

    override fun createStub(psi: KtTypeAlias, parentStub: StubElement<*>): KotlinTypeAliasStubImpl {
        val name = StringRef.fromString(psi.name)
        val fqName = StringRef.fromString(psi.safeFqNameForLazyResolve()?.asString())
        val classId = StubUtils.createNestedClassId(parentStub, psi)
        val isTopLevel = psi.isTopLevel()
        return KotlinTypeAliasStubImpl(parentStub, name, fqName, classId, isTopLevel)
    }

    override fun serialize(stub: KotlinTypeAliasStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeName(stub.fqName?.asString())
        StubUtils.serializeClassId(dataStream, stub.classId)
        dataStream.writeBoolean(stub.isTopLevel)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinTypeAliasStubImpl {
        val name = dataStream.readName()
        val fqName = dataStream.readName()
        val classId = StubUtils.deserializeClassId(dataStream)
        val isTopLevel = dataStream.readBoolean()
        return KotlinTypeAliasStubImpl(parentStub, name, fqName, classId, isTopLevel)
    }

    override fun indexStub(stub: KotlinTypeAliasStubImpl, sink: IndexSink) {
        StubIndexService.getInstance().indexTypeAlias(stub, sink)
    }
}
