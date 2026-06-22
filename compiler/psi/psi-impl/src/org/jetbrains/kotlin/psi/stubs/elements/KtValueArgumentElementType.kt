/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElementFactory
import com.intellij.psi.stubs.StubSerializer
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.stubs.KotlinValueArgumentStub
import org.jetbrains.kotlin.psi.stubs.factory.KotlinValueArgumentStubFactory
import org.jetbrains.kotlin.psi.stubs.factory.KotlinValueArgumentStubSerializer
import org.jetbrains.kotlin.psi.stubs.impl.KotlinValueArgumentStubImpl

class KtValueArgumentElementType<T : KtValueArgument>(debugName: String, psiClass: Class<T>) :
    KtStubElementType<KotlinValueArgumentStubImpl<T>, T>(
        debugName,
        psiClass,
        KotlinValueArgumentStub::class.java,
    ) {
    private val stubFactory = KotlinValueArgumentStubFactory(this)
    private val stubSerializer = KotlinValueArgumentStubSerializer(this)

    override fun getStubFactory(): StubElementFactory<KotlinValueArgumentStubImpl<T>, T> = stubFactory

    override fun getStubSerializer(): StubSerializer<KotlinValueArgumentStubImpl<T>> = stubSerializer
}
