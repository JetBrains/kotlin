/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.stubs.KotlinNameReferenceExpressionStub
import org.jetbrains.kotlin.psi.stubs.KotlinStubElement

@KtImplementationDetail
class KotlinNameReferenceExpressionStubImpl(
    parent: StubElement<*>?,
    private val referencedNameRef: StringRef,
    val isClassRef: Boolean,
) : KotlinStubBaseImpl<KtNameReferenceExpression>(parent, KtNodeTypes.REFERENCE_EXPRESSION), KotlinNameReferenceExpressionStub {
    override val referencedName: String
        get() = referencedNameRef.string

    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinNameReferenceExpressionStubImpl = KotlinNameReferenceExpressionStubImpl(
        parent = newParent,
        referencedNameRef = referencedNameRef,
        isClassRef = isClassRef,
    )

    @KtImplementationDetail
    override fun isEquivalentTo(other: KotlinStubElement<*>): Boolean =
        other is KotlinNameReferenceExpressionStubImpl &&
                other.isClassRef == isClassRef &&
                other.referencedNameRef == referencedNameRef
}
