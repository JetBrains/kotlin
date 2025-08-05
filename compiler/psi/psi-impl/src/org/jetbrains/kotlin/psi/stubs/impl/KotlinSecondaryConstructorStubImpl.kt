/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.stubs.KotlinConstructorStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KotlinSecondaryConstructorStubImpl(
    parent: StubElement<out PsiElement>?,
    private val containingClassName: StringRef?,
    private val hasBody: Boolean,
    private val isDelegatedCallToThis: Boolean,
    private val isExplicitDelegationCall: Boolean,
    private val mayHaveContract: Boolean,
) : KotlinStubBaseImpl<KtSecondaryConstructor>(parent, KtStubElementTypes.SECONDARY_CONSTRUCTOR),
    KotlinConstructorStub<KtSecondaryConstructor> {
    override val fqName: FqName? get() = null
    override fun getName(): String? = StringRef.toString(containingClassName)
    override fun isTopLevel(): Boolean = false
    override fun isExtension(): Boolean = false
    override fun mayHaveContract(): Boolean = mayHaveContract

    // It cannot have expression body
    override fun hasNoExpressionBody(): Boolean = true

    override fun hasBody(): Boolean = hasBody
    override fun isDelegatedCallToThis(): Boolean = isDelegatedCallToThis
    override fun isExplicitDelegationCall(): Boolean = isExplicitDelegationCall
}
