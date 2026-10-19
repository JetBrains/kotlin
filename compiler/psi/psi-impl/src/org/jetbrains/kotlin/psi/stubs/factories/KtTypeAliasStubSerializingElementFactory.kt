/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.lang.LighterAST
import com.intellij.lang.LighterASTNode
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.psiUtil.safeFqNameForLazyResolve
import org.jetbrains.kotlin.psi.stubs.StubUtils
import org.jetbrains.kotlin.psi.stubs.StubUtils.createClassId
import org.jetbrains.kotlin.psi.stubs.StubUtils.deserializeClassId
import org.jetbrains.kotlin.psi.stubs.StubUtils.serializeClassId
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeAliasStubImpl
import org.jetbrains.kotlin.psi.stubs.light.declarationName
import org.jetbrains.kotlin.psi.stubs.light.isTopLevelDeclaration
import org.jetbrains.kotlin.psi.stubs.light.safeFqNameForLazyResolve

internal object KtTypeAliasStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinTypeAliasStubImpl, KtTypeAlias>(
        type = KtNodeTypes.TYPEALIAS,
    ) {

    override fun createPsi(stub: KotlinTypeAliasStubImpl): KtTypeAlias = KtTypeAlias(stub)

    override fun createStub(psi: KtTypeAlias, parentStub: StubElement<*>?): KotlinTypeAliasStubImpl {
        val name = StringRef.fromString(psi.name)
        val fqName = StringRef.fromString(psi.safeFqNameForLazyResolve()?.asString())
        val classId = parentStub?.let { createClassId(it, psi) }
        val isTopLevel = psi.isTopLevel()
        return KotlinTypeAliasStubImpl(
            parent = parentStub,
            name = name,
            qualifiedName = fqName,
            classId = classId,
            isTopLevel = isTopLevel,
        )
    }

    override fun createStub(
        tree: LighterAST,
        node: LighterASTNode,
        parentStub: StubElement<*>,
    ): KotlinTypeAliasStubImpl {
        val name = tree.declarationName(node)
        return KotlinTypeAliasStubImpl(
            parent = parentStub,
            name = StringRef.fromString(name),
            qualifiedName = StringRef.fromString(tree.safeFqNameForLazyResolve(node, parentStub)?.asString()),
            classId = StubUtils.createClassId(parentStub, KtPsiUtil.safeName(name), isEnumEntry = false),
            isTopLevel = tree.isTopLevelDeclaration(node),
        )
    }

    override fun serialize(stub: KotlinTypeAliasStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeName(stub.fqName?.asString())
        serializeClassId(dataStream, stub.classId)
        dataStream.writeBoolean(stub.isTopLevel)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinTypeAliasStubImpl {
        val name = dataStream.readName()
        val fqName = dataStream.readName()
        val classId = deserializeClassId(dataStream)
        val isTopLevel = dataStream.readBoolean()
        return KotlinTypeAliasStubImpl(
            parent = parentStub,
            name = name,
            qualifiedName = fqName,
            classId = classId,
            isTopLevel = isTopLevel,
        )
    }

    override fun indexStub(stub: KotlinTypeAliasStubImpl, sink: IndexSink) {
        StubIndexService.getInstance()?.indexTypeAlias(stub, sink)
    }
}
