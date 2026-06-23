/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElementFactory
import com.intellij.psi.stubs.StubSerializer
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.stubs.KotlinConstructorStub
import org.jetbrains.kotlin.psi.stubs.factory.KotlinSecondaryConstructorStubFactory
import org.jetbrains.kotlin.psi.stubs.factory.KotlinSecondaryConstructorStubSerializer
import org.jetbrains.kotlin.psi.stubs.impl.KotlinSecondaryConstructorStubImpl

class KtSecondaryConstructorElementType(@NonNls debugName: String) :
    KtStubElementType<KotlinSecondaryConstructorStubImpl, KtSecondaryConstructor>(
        /* debugName = */ debugName,
        /* psiClass = */ KtSecondaryConstructor::class.java,
        /* stubClass = */ KotlinConstructorStub::class.java,
    ) {
    @KtImplementationDetail
    override fun getStubFactory(): StubElementFactory<KotlinSecondaryConstructorStubImpl, KtSecondaryConstructor> =
        KotlinSecondaryConstructorStubFactory

    @KtImplementationDetail
    override fun getStubSerializer(): StubSerializer<KotlinSecondaryConstructorStubImpl> =
        KotlinSecondaryConstructorStubSerializer
}
