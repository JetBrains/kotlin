/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtContextReceiver
import org.jetbrains.kotlin.psi.stubs.KotlinContextReceiverStub
import org.jetbrains.kotlin.psi.stubs.elements.KtContextReceiverElementType

class KotlinContextReceiverStubImpl(
    parent: StubElement<out PsiElement>?,
    elementType: KtContextReceiverElementType,
    private val label: String?,
) : KotlinStubBaseImpl<KtContextReceiver>(parent, elementType), KotlinContextReceiverStub {
    override fun getLabel() = label
}
