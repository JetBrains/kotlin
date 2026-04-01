/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.stubs.KotlinConstructorStub
import org.jetbrains.kotlin.psi.stubs.StubUtils.deserializeKdocText
import org.jetbrains.kotlin.psi.stubs.StubUtils.serializeKdocText
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPrimaryConstructorStubImpl
import java.io.IOException

class KtPrimaryConstructorElementType(@NonNls debugName: String) :
    KtStubElementType<KotlinPrimaryConstructorStubImpl, KtPrimaryConstructor>(
        /* debugName = */ debugName,
        /* psiClass = */ KtPrimaryConstructor::class.java,
        /* stubClass = */ KotlinConstructorStub::class.java,
    ) {

    override fun createStub(
        psi: KtPrimaryConstructor,
        parentStub: StubElement<*>,
    ): KotlinPrimaryConstructorStubImpl = KotlinPrimaryConstructorStubImpl(
        parent = parentStub,
        containingClassName = StringRef.fromString(psi.name),
        kdocText = null,
    )

    @Throws(IOException::class)
    override fun serialize(stub: KotlinPrimaryConstructorStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.serializeKdocText(stub.kdocText)
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): KotlinPrimaryConstructorStubImpl {
        val name = dataStream.readName()
        val kdocText = dataStream.deserializeKdocText()
        return KotlinPrimaryConstructorStubImpl(
            parent = parentStub,
            containingClassName = name,
            kdocText = kdocText,
        )
    }
}
