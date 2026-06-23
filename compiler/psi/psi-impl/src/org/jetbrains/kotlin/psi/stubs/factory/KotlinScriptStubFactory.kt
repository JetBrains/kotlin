/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService
import org.jetbrains.kotlin.psi.stubs.impl.KotlinScriptStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinScriptStubFactory : StubElementFactory<KotlinScriptStubImpl, KtScript> {
    @OptIn(KtImplementationDetail::class)
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    @OptIn(KtExperimentalApi::class)
    override fun createStub(psi: KtScript, parentStub: StubElement<out PsiElement>?): KotlinScriptStubImpl {
        return KotlinScriptStubImpl(
            parentStub,
            StringRef.fromString(psi.fqName.asString()),
            isReplSnippet = psi.isReplSnippet,
        )
    }

    override fun createPsi(stub: KotlinScriptStubImpl): KtScript = KtScript(stub)
}

internal object KotlinScriptStubSerializer : StubSerializer<KotlinScriptStubImpl> {
    override fun getExternalId(): String = "kotlin.SCRIPT"

    override fun serialize(stub: KotlinScriptStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.fqName.asString())
        dataStream.writeBoolean(stub.isReplSnippet)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinScriptStubImpl {
        val fqName = dataStream.readName()!!
        val isReplSnippet = dataStream.readBoolean()
        return KotlinScriptStubImpl(parentStub, fqName, isReplSnippet)
    }

    override fun indexStub(stub: KotlinScriptStubImpl, sink: IndexSink) {
        StubIndexService.getInstance().indexScript(stub, sink)
    }
}
