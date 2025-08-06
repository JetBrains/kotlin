/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.psi.KtContextReceiver
import org.jetbrains.kotlin.psi.stubs.KotlinContextReceiverStub
import org.jetbrains.kotlin.psi.stubs.elements.KtContextReceiverElementType

class KotlinContextReceiverStubImpl(
    parent: StubElement<out PsiElement>?,
    elementType: KtContextReceiverElementType,
    private val _label: StringRef?,
) : KotlinStubBaseImpl<KtContextReceiver>(parent, elementType), KotlinContextReceiverStub {
    override val label: String? get() = _label?.string
}
