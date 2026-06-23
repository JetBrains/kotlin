/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElementFactory
import com.intellij.psi.stubs.StubSerializer
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyAccessorStub
import org.jetbrains.kotlin.psi.stubs.factory.KotlinPropertyAccessorStubFactory
import org.jetbrains.kotlin.psi.stubs.factory.KotlinPropertyAccessorStubSerializer
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPropertyAccessorStubImpl

internal object KtPropertyAccessorElementType : KtStubElementType<KotlinPropertyAccessorStubImpl, KtPropertyAccessor>(
    "PROPERTY_ACCESSOR",
    KtPropertyAccessor::class.java,
    KotlinPropertyAccessorStub::class.java,
) {
    @KtImplementationDetail
    override fun getStubFactory(): StubElementFactory<KotlinPropertyAccessorStubImpl, KtPropertyAccessor> = KotlinPropertyAccessorStubFactory

    @KtImplementationDetail
    override fun getStubSerializer(): StubSerializer<KotlinPropertyAccessorStubImpl> = KotlinPropertyAccessorStubSerializer
}
