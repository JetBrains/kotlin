/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.psi.util.childrenOfType
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.stubs.KotlinBlockStringTemplateEntryStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinBlockStringTemplateEntryStubImpl

class KtBlockStringTemplateEntryElementType(@NonNls debugName: String) :
    KtStubElementType<KotlinBlockStringTemplateEntryStubImpl, KtBlockStringTemplateEntry>(
        debugName,
        KtBlockStringTemplateEntry::class.java,
        KotlinBlockStringTemplateEntryStub::class.java,
    ) {

    override fun createStub(
        psi: KtBlockStringTemplateEntry,
        parentStub: StubElement<*>?,
    ): KotlinBlockStringTemplateEntryStubImpl = KotlinBlockStringTemplateEntryStubImpl(
        parentStub,
        hasMultipleExpressions = psi.childrenOfType<KtExpression>().size > 1,
        text = psi.text,
    )

    override fun serialize(
        stub: KotlinBlockStringTemplateEntryStubImpl,
        dataStream: StubOutputStream,
    ) {
        dataStream.writeBoolean(stub.hasMultipleExpressions)
        dataStream.writeUTFFast(stub.text)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinBlockStringTemplateEntryStubImpl {
        val hasMultipleExpressions = dataStream.readBoolean()
        val text = dataStream.readUTFFast()
        return KotlinBlockStringTemplateEntryStubImpl(
            parent = parentStub,
            hasMultipleExpressions = hasMultipleExpressions,
            text = text,
        )
    }
}
