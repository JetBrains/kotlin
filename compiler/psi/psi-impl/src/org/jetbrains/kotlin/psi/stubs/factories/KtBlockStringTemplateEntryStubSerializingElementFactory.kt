/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.psi.util.childrenOfType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.impl.KotlinBlockStringTemplateEntryStubImpl

internal object KtBlockStringTemplateEntryStubSerializingElementFactory :
    KtStubSerializingElementFactory<KotlinBlockStringTemplateEntryStubImpl, KtBlockStringTemplateEntry>(
        type = KtNodeTypes.LONG_STRING_TEMPLATE_ENTRY,
    ) {

    override fun createPsi(
        stub: KotlinBlockStringTemplateEntryStubImpl,
    ): KtBlockStringTemplateEntry = KtBlockStringTemplateEntry(stub)

    override fun createStub(
        psi: KtBlockStringTemplateEntry,
        parentStub: StubElement<*>?,
    ): KotlinBlockStringTemplateEntryStubImpl = KotlinBlockStringTemplateEntryStubImpl(
        parent = parentStub,
        hasMultipleExpressions = psi.childrenOfType<KtExpression>().size > 1,
        text = psi.text,
    )

    override fun serialize(stub: KotlinBlockStringTemplateEntryStubImpl, dataStream: StubOutputStream) {
        dataStream.writeBoolean(stub.hasMultipleExpressions)
        dataStream.writeUTFFast(stub.text)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinBlockStringTemplateEntryStubImpl = KotlinBlockStringTemplateEntryStubImpl(
        parent = parentStub,
        hasMultipleExpressions = dataStream.readBoolean(),
        text = dataStream.readUTFFast(),
    )
}
