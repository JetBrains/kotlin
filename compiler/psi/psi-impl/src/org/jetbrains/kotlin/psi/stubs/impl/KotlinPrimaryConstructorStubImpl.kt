/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.stubs.KotlinConstructorStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KotlinPrimaryConstructorStubImpl(
    parent: StubElement<out PsiElement>?,
    private val containingClassName: StringRef?,
) : KotlinStubBaseImpl<KtPrimaryConstructor>(parent, KtStubElementTypes.PRIMARY_CONSTRUCTOR),
    KotlinConstructorStub<KtPrimaryConstructor> {
    override val fqName: FqName? get() = null
    override fun getName(): String? = StringRef.toString(containingClassName)
    override fun isTopLevel(): Boolean = false
    override fun isExtension(): Boolean = false
    override val mayHaveContract: Boolean get() = false
    override val hasNoExpressionBody: Boolean get() = true
    override val hasBody: Boolean get() = false
    override fun isDelegatedCallToThis(): Boolean = false
    override fun isExplicitDelegationCall(): Boolean = false
}
