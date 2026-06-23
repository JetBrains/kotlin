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
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.psiUtil.safeFqNameForLazyResolve
import org.jetbrains.kotlin.psi.stubs.StubUtils
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeAliasStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinTypeAliasStubFactory : StubElementFactory<KotlinTypeAliasStubImpl, KtTypeAlias> {
    @OptIn(KtImplementationDetail::class)
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(psi: KtTypeAlias, parentStub: StubElement<out PsiElement>?): KotlinTypeAliasStubImpl {
        val name = StringRef.fromString(psi.name)
        val fqName = StringRef.fromString(psi.safeFqNameForLazyResolve()?.asString())
        val classId = StubUtils.createClassId(parentStub!!, psi)
        val isTopLevel = psi.isTopLevel()
        return KotlinTypeAliasStubImpl(parentStub, name, fqName, classId, isTopLevel)
    }

    override fun createPsi(stub: KotlinTypeAliasStubImpl): KtTypeAlias = KtTypeAlias(stub)
}

internal object KotlinTypeAliasStubSerializer : StubSerializer<KotlinTypeAliasStubImpl> {
    override fun getExternalId(): String = "kotlin.TYPEALIAS"

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
