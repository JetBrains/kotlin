/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElementFactory
import com.intellij.psi.stubs.StubSerializer
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtElementImplStub
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderWithTextStub
import org.jetbrains.kotlin.psi.stubs.factory.KotlinPlaceHolderWithTextStubFactory
import org.jetbrains.kotlin.psi.stubs.factory.KotlinPlaceHolderWithTextStubSerializer
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderWithTextStubImpl

class KtPlaceHolderWithTextStubElementType<T : KtElementImplStub<*>>(@NonNls debugName: String, psiClass: Class<T>) :
    KtStubElementType<KotlinPlaceHolderWithTextStubImpl<T>, T>(
        debugName,
        psiClass,
        KotlinPlaceHolderWithTextStub::class.java,
    ) {
    private val stubFactory = KotlinPlaceHolderWithTextStubFactory(this)
    private val stubSerializer = KotlinPlaceHolderWithTextStubSerializer(this)

    @KtImplementationDetail
    override fun getStubFactory(): StubElementFactory<KotlinPlaceHolderWithTextStubImpl<T>, T> = stubFactory

    @KtImplementationDetail
    override fun getStubSerializer(): StubSerializer<KotlinPlaceHolderWithTextStubImpl<T>> = stubSerializer
}
