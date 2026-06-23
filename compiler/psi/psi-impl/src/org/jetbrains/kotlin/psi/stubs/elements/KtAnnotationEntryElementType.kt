/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElementFactory
import com.intellij.psi.stubs.StubSerializer
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinAnnotationEntryStub
import org.jetbrains.kotlin.psi.stubs.factory.KotlinAnnotationEntryStubFactory
import org.jetbrains.kotlin.psi.stubs.factory.KotlinAnnotationEntryStubSerializer
import org.jetbrains.kotlin.psi.stubs.impl.KotlinAnnotationEntryStubImpl

internal object KtAnnotationEntryElementType : KtStubElementType<KotlinAnnotationEntryStubImpl, KtAnnotationEntry>(
    "ANNOTATION_ENTRY",
    KtAnnotationEntry::class.java,
    KotlinAnnotationEntryStub::class.java,
) {
    @KtImplementationDetail
    override fun getStubFactory(): StubElementFactory<KotlinAnnotationEntryStubImpl, KtAnnotationEntry> =
        KotlinAnnotationEntryStubFactory

    @KtImplementationDetail
    override fun getStubSerializer(): StubSerializer<KotlinAnnotationEntryStubImpl> = KotlinAnnotationEntryStubSerializer
}
