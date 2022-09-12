/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.stubs.KotlinConstructorStub
import org.jetbrains.kotlin.psi.stubs.elements.StubIndexService.Companion.getInstance
import java.io.IOException

abstract class KtConstructorElementType<T : KtConstructor<T>>(
    @NonNls debugName: String,
    tClass: Class<T>,
    stubClass: Class<KotlinConstructorStub<*>>
) : KtStubElementType<KotlinConstructorStub<T>, T>(debugName, tClass, stubClass) {
    protected abstract fun newStub(
        parentStub: StubElement<*>,
        nameRef: StringRef?,
        hasBlockBody: Boolean,
        hasBody: Boolean
    ): KotlinConstructorStub<T>

    override fun createStub(psi: T, parentStub: StubElement<*>): KotlinConstructorStub<T> {
        val hasBlockBody = psi.hasBlockBody()
        val hasBody = psi.hasBody()
        return newStub(parentStub, StringRef.fromString(psi.name), hasBlockBody, hasBody)
    }

    @Throws(IOException::class)
    override fun serialize(stub: KotlinConstructorStub<T>, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(stub.hasBlockBody())
        dataStream.writeBoolean(stub.hasBody())
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): KotlinConstructorStub<T> {
        val name = dataStream.readName()
        val hasBlockBody = dataStream.readBoolean()
        val hasBody = dataStream.readBoolean()
        return newStub(parentStub, name, hasBlockBody, hasBody)
    }

    override fun indexStub(stub: KotlinConstructorStub<T>, sink: IndexSink) {
        getInstance().indexConstructor(stub, sink)
    }
}