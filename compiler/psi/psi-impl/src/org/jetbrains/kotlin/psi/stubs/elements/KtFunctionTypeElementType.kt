/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElementFactory
import com.intellij.psi.stubs.StubSerializer
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.stubs.KotlinFunctionTypeStub
import org.jetbrains.kotlin.psi.stubs.factory.KotlinFunctionTypeStubFactory
import org.jetbrains.kotlin.psi.stubs.factory.KotlinFunctionTypeStubSerializer
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFunctionTypeStubImpl

class KtFunctionTypeElementType(@NonNls debugName: String) : KtStubElementType<KotlinFunctionTypeStubImpl, KtFunctionType>(
    debugName,
    KtFunctionType::class.java,
    KotlinFunctionTypeStub::class.java,
) {
    override fun getStubFactory(): StubElementFactory<KotlinFunctionTypeStubImpl, KtFunctionType> =
        KotlinFunctionTypeStubFactory

    override fun getStubSerializer(): StubSerializer<KotlinFunctionTypeStubImpl> = KotlinFunctionTypeStubSerializer
}
