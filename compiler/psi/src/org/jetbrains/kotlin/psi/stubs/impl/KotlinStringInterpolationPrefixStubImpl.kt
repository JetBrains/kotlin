/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.KtStringInterpolationPrefix
import org.jetbrains.kotlin.psi.stubs.KotlinStringInterpolationPrefixStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KotlinStringInterpolationPrefixStubImpl(
    parent: StubElement<out PsiElement>?,
    override val dollarSignCount: Int,
) : KotlinStubBaseImpl<KtStringInterpolationPrefix>(parent, KtStubElementTypes.STRING_INTERPOLATION_PREFIX),
    KotlinStringInterpolationPrefixStub