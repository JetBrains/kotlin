/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElementFactory
import com.intellij.psi.stubs.StubSerializer
import org.jetbrains.kotlin.psi.KtAnnotationUseSiteTarget
import org.jetbrains.kotlin.psi.stubs.KotlinAnnotationUseSiteTargetStub
import org.jetbrains.kotlin.psi.stubs.factory.KotlinAnnotationUseSiteTargetStubFactory
import org.jetbrains.kotlin.psi.stubs.factory.KotlinAnnotationUseSiteTargetStubSerializer
import org.jetbrains.kotlin.psi.stubs.impl.KotlinAnnotationUseSiteTargetStubImpl

class KtAnnotationUseSiteTargetElementType(debugName: String) :
    KtStubElementType<KotlinAnnotationUseSiteTargetStubImpl, KtAnnotationUseSiteTarget>(
        debugName,
        KtAnnotationUseSiteTarget::class.java,
        KotlinAnnotationUseSiteTargetStub::class.java,
    ) {
    override fun getStubFactory(): StubElementFactory<KotlinAnnotationUseSiteTargetStubImpl, KtAnnotationUseSiteTarget> =
        KotlinAnnotationUseSiteTargetStubFactory

    override fun getStubSerializer(): StubSerializer<KotlinAnnotationUseSiteTargetStubImpl> =
        KotlinAnnotationUseSiteTargetStubSerializer
}
