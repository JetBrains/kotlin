/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElementFactory
import com.intellij.psi.stubs.StubSerializer
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtStringInterpolationPrefix
import org.jetbrains.kotlin.psi.stubs.KotlinStringInterpolationPrefixStub
import org.jetbrains.kotlin.psi.stubs.factory.KotlinStringInterpolationPrefixStubFactory
import org.jetbrains.kotlin.psi.stubs.factory.KotlinStringInterpolationPrefixStubSerializer
import org.jetbrains.kotlin.psi.stubs.impl.KotlinStringInterpolationPrefixStubImpl

class KtStringInterpolationPrefixElementType(debugName: String) :
    KtStubElementType<KotlinStringInterpolationPrefixStubImpl, KtStringInterpolationPrefix>(
        debugName,
        KtStringInterpolationPrefix::class.java,
        KotlinStringInterpolationPrefixStub::class.java,
    ) {
    @KtImplementationDetail
    override fun getStubFactory(): StubElementFactory<KotlinStringInterpolationPrefixStubImpl, KtStringInterpolationPrefix> =
        KotlinStringInterpolationPrefixStubFactory

    @KtImplementationDetail
    override fun getStubSerializer(): StubSerializer<KotlinStringInterpolationPrefixStubImpl> =
        KotlinStringInterpolationPrefixStubSerializer
}
