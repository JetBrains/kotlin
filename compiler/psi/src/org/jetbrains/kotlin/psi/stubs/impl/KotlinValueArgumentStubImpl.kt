/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.stubs.KotlinValueArgumentStub
import org.jetbrains.kotlin.psi.stubs.elements.KtValueArgumentElementType

class KotlinValueArgumentStubImpl<T : KtValueArgument>(
    parent: StubElement<out PsiElement>?,
    elementType: KtValueArgumentElementType<T>,
    private val isSpread: Boolean
) : KotlinPlaceHolderStubImpl<T>(parent, elementType), KotlinValueArgumentStub<T> {
    override fun isSpread(): Boolean = isSpread
}