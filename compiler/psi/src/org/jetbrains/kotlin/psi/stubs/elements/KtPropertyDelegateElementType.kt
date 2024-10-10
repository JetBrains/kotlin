/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyDelegateStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPropertyDelegateStubImpl

class KtPropertyDelegateElementType(debugName: String) : KtStubElementType<KotlinPropertyDelegateStub, KtPropertyDelegate>(
    debugName,
    KtPropertyDelegate::class.java,
    KotlinPropertyDelegateStub::class.java,
) {
    override fun createStub(psi: KtPropertyDelegate, parentStub: StubElement<*>): KotlinPropertyDelegateStub {
        return KotlinPropertyDelegateStubImpl(parentStub, psi.expression != null)
    }

    override fun serialize(stub: KotlinPropertyDelegateStub, dataStream: StubOutputStream) {
        dataStream.writeBoolean(stub.hasExpression());
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): KotlinPropertyDelegateStub {
        val hasExpression = dataStream.readBoolean()
        return KotlinPropertyDelegateStubImpl(parent = parentStub, hasExpression = hasExpression)
    }
}
