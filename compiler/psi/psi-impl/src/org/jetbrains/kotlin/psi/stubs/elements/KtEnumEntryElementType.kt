/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElementFactory
import com.intellij.psi.stubs.StubSerializer
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinClassStub
import org.jetbrains.kotlin.psi.stubs.factory.KotlinEnumEntryStubFactory
import org.jetbrains.kotlin.psi.stubs.factory.KotlinEnumEntryStubSerializer
import org.jetbrains.kotlin.psi.stubs.impl.KotlinEnumEntryStubImpl

internal object KtEnumEntryElementType : KtStubElementType<KotlinEnumEntryStubImpl, KtEnumEntry>(
    "ENUM_ENTRY",
    KtEnumEntry::class.java,
    KotlinClassStub::class.java,
) {
    @KtImplementationDetail
    override fun getStubFactory(): StubElementFactory<KotlinEnumEntryStubImpl, KtEnumEntry> = KotlinEnumEntryStubFactory

    @KtImplementationDetail
    override fun getStubSerializer(): StubSerializer<KotlinEnumEntryStubImpl> = KotlinEnumEntryStubSerializer
}
