/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.factory

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtEnumEntrySuperclassReferenceExpression
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.psi.stubs.impl.KotlinEnumEntrySuperclassReferenceExpressionStubImpl

@Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
internal object KotlinEnumEntrySuperclassReferenceExpressionStubFactory :
    StubElementFactory<KotlinEnumEntrySuperclassReferenceExpressionStubImpl, KtEnumEntrySuperclassReferenceExpression> {
    override fun shouldCreateStub(node: ASTNode): Boolean = KtStubElementType.shouldCreateStubDependingOnParent(node)

    override fun createStub(
        psi: KtEnumEntrySuperclassReferenceExpression,
        parentStub: StubElement<out PsiElement>?,
    ): KotlinEnumEntrySuperclassReferenceExpressionStubImpl = KotlinEnumEntrySuperclassReferenceExpressionStubImpl(
        parent = parentStub,
        referencedNameRef = StringRef.fromString(psi.getReferencedName()),
    )

    override fun createPsi(stub: KotlinEnumEntrySuperclassReferenceExpressionStubImpl): KtEnumEntrySuperclassReferenceExpression =
        KtEnumEntrySuperclassReferenceExpression(stub)
}

internal object KotlinEnumEntrySuperclassReferenceExpressionStubSerializer :
    StubSerializer<KotlinEnumEntrySuperclassReferenceExpressionStubImpl> {
    override fun getExternalId(): String = "kotlin.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION"

    override fun serialize(stub: KotlinEnumEntrySuperclassReferenceExpressionStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.referencedName)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>?,
    ): KotlinEnumEntrySuperclassReferenceExpressionStubImpl = KotlinEnumEntrySuperclassReferenceExpressionStubImpl(
        parent = parentStub,
        referencedNameRef = dataStream.readName()!!,
    )

    override fun indexStub(stub: KotlinEnumEntrySuperclassReferenceExpressionStubImpl, sink: IndexSink) {
        // not indexed
    }
}
