/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import com.intellij.psi.util.childrenOfType
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.impl.KotlinBlockStringTemplateEntryStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinBlockStringTemplateEntryStubFactory :
    StubElementFactory<KotlinBlockStringTemplateEntryStubImpl, KtBlockStringTemplateEntry> {
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(
        psi: KtBlockStringTemplateEntry,
        parentStub: StubElement<out PsiElement>?,
    ): KotlinBlockStringTemplateEntryStubImpl = KotlinBlockStringTemplateEntryStubImpl(
        parentStub,
        hasMultipleExpressions = psi.childrenOfType<KtExpression>().size > 1,
        text = psi.text,
    )

    override fun createPsi(stub: KotlinBlockStringTemplateEntryStubImpl): KtBlockStringTemplateEntry =
        KtBlockStringTemplateEntry(stub)
}

internal object KotlinBlockStringTemplateEntryStubSerializer : StubSerializer<KotlinBlockStringTemplateEntryStubImpl> {
    override fun getExternalId(): String = "kotlin.LONG_STRING_TEMPLATE_ENTRY"

    override fun serialize(stub: KotlinBlockStringTemplateEntryStubImpl, dataStream: StubOutputStream) {
        dataStream.writeBoolean(stub.hasMultipleExpressions)
        dataStream.writeUTFFast(stub.text)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): KotlinBlockStringTemplateEntryStubImpl {
        val hasMultipleExpressions = dataStream.readBoolean()
        val text = dataStream.readUTFFast()
        return KotlinBlockStringTemplateEntryStubImpl(
            parent = parentStub,
            hasMultipleExpressions = hasMultipleExpressions,
            text = text,
        )
    }

    override fun indexStub(stub: KotlinBlockStringTemplateEntryStubImpl, sink: IndexSink) {
        // not indexed
    }
}
