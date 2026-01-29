/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.stubs.KotlinConstructorStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

@OptIn(KtImplementationDetail::class)
class KotlinSecondaryConstructorStubImpl(
    parent: StubElement<*>?,
    private val containingClassName: StringRef?,
    override val hasBody: Boolean,
    override val isDelegatedCallToThis: Boolean,
    override val isExplicitDelegationCall: Boolean,
    override val mayHaveContract: Boolean,
    override val kdocText: String?,
) : KotlinStubBaseImpl<KtSecondaryConstructor>(parent, KtStubElementTypes.SECONDARY_CONSTRUCTOR),
    KotlinConstructorStub<KtSecondaryConstructor> {
    override val fqName: FqName? get() = null
    override fun getName(): String? = StringRef.toString(containingClassName)
    override val isTopLevel: Boolean get() = false
    override val isExtension: Boolean get() = false

    // It cannot have expression body
    override val hasNoExpressionBody: Boolean
        get() = true

    @KtImplementationDetail
    override fun copyInto(newParent: StubElement<*>?): KotlinSecondaryConstructorStubImpl = KotlinSecondaryConstructorStubImpl(
        parent = newParent,
        containingClassName = containingClassName,
        hasBody = hasBody,
        isDelegatedCallToThis = isDelegatedCallToThis,
        isExplicitDelegationCall = isExplicitDelegationCall,
        mayHaveContract = mayHaveContract,
        kdocText = kdocText,
    )
}
