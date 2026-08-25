/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(KtImplementationDetail::class, KtExperimentalApi::class)

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtExperimentalApi
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService
import org.jetbrains.kotlin.psi.stubs.impl.KotlinScriptStubImpl

internal object KtScriptStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinScriptStubImpl, KtScript>(
        type = KtNodeTypes.SCRIPT,
    ) {

    override fun createPsi(stub: KotlinScriptStubImpl): KtScript = KtScript(stub)

    override fun createStub(psi: KtScript, parentStub: StubElement<*>?): KotlinScriptStubImpl = KotlinScriptStubImpl(
        parent = parentStub,
        fqNameRef = StringRef.fromString(psi.fqName.asString())!!,
        isReplSnippet = psi.isReplSnippet,
    )

    override fun serialize(stub: KotlinScriptStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.fqName.asString())
        dataStream.writeBoolean(stub.isReplSnippet)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinScriptStubImpl {
        val fqName = dataStream.readName()!!
        val isReplSnippet = dataStream.readBoolean()

        return KotlinScriptStubImpl(
            parent = parentStub,
            fqNameRef = fqName,
            isReplSnippet = isReplSnippet,
        )
    }

    override fun indexStub(stub: KotlinScriptStubImpl, sink: IndexSink) {
        StubIndexService.getInstance().indexScript(stub, sink)
    }
}
