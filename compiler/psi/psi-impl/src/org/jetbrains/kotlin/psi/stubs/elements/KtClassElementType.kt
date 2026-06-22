/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElementFactory
import com.intellij.psi.stubs.StubSerializer
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.stubs.KotlinClassStub
import org.jetbrains.kotlin.psi.stubs.factory.KotlinClassStubFactory
import org.jetbrains.kotlin.psi.stubs.factory.KotlinClassStubSerializer
import org.jetbrains.kotlin.psi.stubs.impl.KotlinClassStubImpl

internal object KtClassElementType : KtStubElementType<KotlinClassStubImpl, KtClass>(
    /* debugName = */ "CLASS",
    /* psiClass = */ KtClass::class.java,
    /* stubClass = */ KotlinClassStub::class.java,
) {
    override fun getStubFactory(): StubElementFactory<KotlinClassStubImpl, KtClass> = KotlinClassStubFactory

    override fun getStubSerializer(): StubSerializer<KotlinClassStubImpl> = KotlinClassStubSerializer
}
