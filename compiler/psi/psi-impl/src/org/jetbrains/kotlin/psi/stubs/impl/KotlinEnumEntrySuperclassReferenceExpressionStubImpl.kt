/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtEnumEntrySuperclassReferenceExpression
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinEnumEntrySuperclassReferenceExpressionStub
import org.jetbrains.kotlin.psi.stubs.KotlinStubElement

@OptIn(KtImplementationDetail::class)
class KotlinEnumEntrySuperclassReferenceExpressionStubImpl(
    parent: StubElement<*>?,
    private val referencedNameRef: StringRef,
) : KotlinStubBaseImpl<KtEnumEntrySuperclassReferenceExpression>(parent, KtNodeTypes.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION),
    KotlinEnumEntrySuperclassReferenceExpressionStub {
    override val referencedName: String get() = referencedNameRef.string

    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinEnumEntrySuperclassReferenceExpressionStubImpl =
        KotlinEnumEntrySuperclassReferenceExpressionStubImpl(
            parent = newParent,
            referencedNameRef = referencedNameRef,
        )

    @KtImplementationDetail
    override fun isEquivalentTo(other: KotlinStubElement<*>): Boolean =
        other is KotlinEnumEntrySuperclassReferenceExpressionStubImpl &&
                other.referencedNameRef == referencedNameRef
}
