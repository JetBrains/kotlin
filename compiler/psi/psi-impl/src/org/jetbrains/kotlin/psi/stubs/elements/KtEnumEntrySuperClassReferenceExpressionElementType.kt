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
import org.jetbrains.kotlin.psi.KtEnumEntrySuperclassReferenceExpression
import org.jetbrains.kotlin.psi.stubs.KotlinEnumEntrySuperclassReferenceExpressionStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinEnumEntrySuperclassReferenceExpressionStubImpl

class KtEnumEntrySuperClassReferenceExpressionElementType(@NonNls debugName: String) :
    KtStubElementType<KotlinEnumEntrySuperclassReferenceExpressionStubImpl, KtEnumEntrySuperclassReferenceExpression>(
        debugName,
        KtEnumEntrySuperclassReferenceExpression::class.java,
        KotlinEnumEntrySuperclassReferenceExpressionStub::class.java,
    ) {

    override fun createStub(
        psi: KtEnumEntrySuperclassReferenceExpression,
        parentStub: StubElement<*>,
    ): KotlinEnumEntrySuperclassReferenceExpressionStubImpl = KotlinEnumEntrySuperclassReferenceExpressionStubImpl(
        parent = parentStub,
        _referencedName = StringRef.fromString(psi.getReferencedName())!!,
    )

    override fun serialize(stub: KotlinEnumEntrySuperclassReferenceExpressionStubImpl, dataStream: StubOutputStream) {
        dataStream.writeName(stub.referencedName)
    }

    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>,
    ): KotlinEnumEntrySuperclassReferenceExpressionStubImpl {
        return KotlinEnumEntrySuperclassReferenceExpressionStubImpl(parentStub, dataStream.readName()!!)
    }
}
