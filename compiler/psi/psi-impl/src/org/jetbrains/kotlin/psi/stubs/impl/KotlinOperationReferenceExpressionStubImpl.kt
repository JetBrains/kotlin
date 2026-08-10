/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IElementType
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.psi.stubs.KotlinOperationReferenceExpressionStub
import org.jetbrains.kotlin.psi.stubs.KotlinStubElement
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

/**
 * @see org.jetbrains.kotlin.psi.KtOperationReferenceExpression
 */
@KtImplementationDetail
class KotlinOperationReferenceExpressionStubImpl(
    parent: StubElement<*>?,
    private val referencedNameRef: StringRef,
    override val operationToken: IElementType,
) : KotlinStubBaseImpl<KtOperationReferenceExpression>(parent, KtStubElementTypes.OPERATION_REFERENCE),
    KotlinOperationReferenceExpressionStub {
    override val referencedName: String
        get() = referencedNameRef.string

    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinOperationReferenceExpressionStubImpl =
        KotlinOperationReferenceExpressionStubImpl(
            parent = newParent,
            referencedNameRef = referencedNameRef,
            operationToken = operationToken,
        )

    @KtImplementationDetail
    override fun isEquivalentTo(other: KotlinStubElement<*>): Boolean =
        other is KotlinOperationReferenceExpressionStubImpl &&
                other.referencedNameRef == referencedNameRef &&
                other.operationToken == operationToken
}
