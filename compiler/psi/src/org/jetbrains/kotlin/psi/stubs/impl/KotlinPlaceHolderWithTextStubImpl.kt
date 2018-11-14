/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.psi.KtElementImplStub
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderWithTextStub

class KotlinPlaceHolderWithTextStubImpl<T : KtElementImplStub<out StubElement<*>>>(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    private val text: String
) : KotlinStubBaseImpl<T>(parent, elementType), KotlinPlaceHolderWithTextStub<T> {
    override fun text(): String = text
}
