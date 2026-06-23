/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElementFactory
import com.intellij.psi.stubs.StubSerializer
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.psi.stubs.KotlinImportAliasStub
import org.jetbrains.kotlin.psi.stubs.factory.KotlinImportAliasStubFactory
import org.jetbrains.kotlin.psi.stubs.factory.KotlinImportAliasStubSerializer
import org.jetbrains.kotlin.psi.stubs.impl.KotlinImportAliasStubImpl

class KtImportAliasElementType(debugName: String) :
    KtStubElementType<KotlinImportAliasStubImpl, KtImportAlias>(
        debugName,
        KtImportAlias::class.java,
        KotlinImportAliasStub::class.java,
    ) {
    @KtImplementationDetail
    override fun getStubFactory(): StubElementFactory<KotlinImportAliasStubImpl, KtImportAlias> = KotlinImportAliasStubFactory

    @KtImplementationDetail
    override fun getStubSerializer(): StubSerializer<KotlinImportAliasStubImpl> = KotlinImportAliasStubSerializer
}
