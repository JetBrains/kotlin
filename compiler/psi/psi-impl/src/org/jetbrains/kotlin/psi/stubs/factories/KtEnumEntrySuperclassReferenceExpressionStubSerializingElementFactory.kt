/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.psi.stubs.factories

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtEnumEntrySuperclassReferenceExpression
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.impl.KotlinEnumEntrySuperclassReferenceExpressionStubImpl

internal object KtEnumEntrySuperclassReferenceExpressionStubSerializingElementFactory :
    KtStubSerializingElementFactory<
            KotlinEnumEntrySuperclassReferenceExpressionStubImpl,
            KtEnumEntrySuperclassReferenceExpression,
            >(
        type = KtNodeTypes.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION,
    ) {

    override fun createPsi(
        stub: KotlinEnumEntrySuperclassReferenceExpressionStubImpl,
    ): KtEnumEntrySuperclassReferenceExpression = KtEnumEntrySuperclassReferenceExpression(stub)

    override fun createStub(
        psi: KtEnumEntrySuperclassReferenceExpression,
        parentStub: StubElement<*>?,
    ): KotlinEnumEntrySuperclassReferenceExpressionStubImpl = KotlinEnumEntrySuperclassReferenceExpressionStubImpl(
        parent = parentStub,
        referencedNameRef = StringRef.fromString(psi.getReferencedName())!!,
    )

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
}
