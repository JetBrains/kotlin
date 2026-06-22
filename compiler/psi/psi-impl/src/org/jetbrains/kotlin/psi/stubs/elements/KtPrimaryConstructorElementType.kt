/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElementFactory
import com.intellij.psi.stubs.StubSerializer
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.stubs.KotlinConstructorStub
import org.jetbrains.kotlin.psi.stubs.factory.KotlinPrimaryConstructorStubFactory
import org.jetbrains.kotlin.psi.stubs.factory.KotlinPrimaryConstructorStubSerializer
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPrimaryConstructorStubImpl

class KtPrimaryConstructorElementType(@NonNls debugName: String) :
    KtStubElementType<KotlinPrimaryConstructorStubImpl, KtPrimaryConstructor>(
        /* debugName = */ debugName,
        /* psiClass = */ KtPrimaryConstructor::class.java,
        /* stubClass = */ KotlinConstructorStub::class.java,
    ) {
    override fun getStubFactory(): StubElementFactory<KotlinPrimaryConstructorStubImpl, KtPrimaryConstructor> =
        KotlinPrimaryConstructorStubFactory

    override fun getStubSerializer(): StubSerializer<KotlinPrimaryConstructorStubImpl> =
        KotlinPrimaryConstructorStubSerializer
}
