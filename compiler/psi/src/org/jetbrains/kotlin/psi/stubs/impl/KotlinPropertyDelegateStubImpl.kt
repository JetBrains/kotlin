/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyDelegateStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KotlinPropertyDelegateStubImpl(
    parent: StubElement<*>,
    private val hasExpression: Boolean,
) : KotlinStubBaseImpl<KtPropertyDelegate>(parent, KtStubElementTypes.PROPERTY_DELEGATE), KotlinPropertyDelegateStub {
    override fun hasExpression(): Boolean = hasExpression
}
