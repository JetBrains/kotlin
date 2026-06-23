/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElementFactory
import com.intellij.psi.stubs.StubSerializer
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.stubs.KotlinTypeAliasStub
import org.jetbrains.kotlin.psi.stubs.factory.KotlinTypeAliasStubFactory
import org.jetbrains.kotlin.psi.stubs.factory.KotlinTypeAliasStubSerializer
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeAliasStubImpl

class KtTypeAliasElementType(debugName: String) :
    KtStubElementType<KotlinTypeAliasStubImpl, KtTypeAlias>(debugName, KtTypeAlias::class.java, KotlinTypeAliasStub::class.java) {
    @KtImplementationDetail
    override fun getStubFactory(): StubElementFactory<KotlinTypeAliasStubImpl, KtTypeAlias> = KotlinTypeAliasStubFactory

    @KtImplementationDetail
    override fun getStubSerializer(): StubSerializer<KotlinTypeAliasStubImpl> = KotlinTypeAliasStubSerializer
}
