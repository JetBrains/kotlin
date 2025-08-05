/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.stubs.KotlinTypeParameterStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KotlinTypeParameterStubImpl(
    parent: StubElement<out PsiElement>?,
    private val name: StringRef?,
    private val isInVariance: Boolean,
    private val isOutVariance: Boolean
) : KotlinStubBaseImpl<KtTypeParameter>(parent, KtStubElementTypes.TYPE_PARAMETER), KotlinTypeParameterStub {
    override fun isInVariance() = isInVariance
    override fun isOutVariance() = isOutVariance
    override fun getName() = StringRef.toString(name)
    // type parameters don't have FqNames
    override val fqName: FqName? get() = null
}
