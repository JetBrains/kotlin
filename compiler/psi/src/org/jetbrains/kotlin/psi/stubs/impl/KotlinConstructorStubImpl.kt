/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.stubs.KotlinConstructorStub
import org.jetbrains.kotlin.psi.stubs.elements.KtConstructorElementType

class KotlinConstructorStubImpl<T : KtConstructor<T>>(
    parent: StubElement<out PsiElement>?,
    elementType: KtConstructorElementType<T>,
    private val containingClassName: StringRef?,
    private val hasBlockBody: Boolean,
    private val hasBody: Boolean,
) : KotlinStubBaseImpl<T>(parent, elementType), KotlinConstructorStub<T> {
    override fun getFqName() = null
    override fun getName() = StringRef.toString(containingClassName)
    override fun isTopLevel() = false
    override fun isExtension() = false
    override fun hasBlockBody() = hasBlockBody
    override fun hasBody() = hasBody
}
