/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.stubs.KotlinFunctionTypeStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

/**
 * @param abbreviatedType The type alias application from which this type was originally expanded. It can be used to render or navigate to
 *  the original type alias instead of the expanded type.
 */
class KotlinFunctionTypeStubImpl(
    parent: StubElement<out PsiElement>?,
    val abbreviatedType: KotlinClassTypeBean? = null,
) : KotlinStubBaseImpl<KtFunctionType>(parent, KtStubElementTypes.FUNCTION_TYPE), KotlinFunctionTypeStub
