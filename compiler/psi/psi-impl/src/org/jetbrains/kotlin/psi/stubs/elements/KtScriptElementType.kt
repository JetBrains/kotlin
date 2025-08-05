/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.stubs.KotlinScriptStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinScriptStubImpl

class KtScriptElementType(debugName: String) : KtStubElementType<KotlinScriptStubImpl, KtScript>(
    debugName,
    KtScript::class.java,
    KotlinScriptStub::class.java,
) {
    override fun createStub(psi: KtScript, parentStub: StubElement<out PsiElement>): KotlinScriptStubImpl {
        return KotlinScriptStubImpl(parentStub, StringRef.fromString(psi.fqName.asString()))
    }

    override fun serialize(stub: KotlinScriptStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.fqName.asString())
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<PsiElement>): KotlinScriptStubImpl {
        val fqName = dataStream.readName()
        return KotlinScriptStubImpl(parentStub, fqName)
    }


    override fun indexStub(stub: KotlinScriptStubImpl, sink: IndexSink) {
        StubIndexService.getInstance().indexScript(stub, sink)
    }
}
