/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.StubElementFactory
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.jetbrains.kotlin.psi.stubs.factory.KotlinPlaceHolderStubFactory
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderStubImpl

object KtScriptInitializerElementType : KtPlaceHolderStubElementType<KtScriptInitializer>(
    "SCRIPT_INITIALIZER", KtScriptInitializer::class.java,
) {
    private val stubFactory = object : KotlinPlaceHolderStubFactory<KtScriptInitializer>(this) {
        override fun shouldCreateStub(node: ASTNode): Boolean = true
    }

    @KtImplementationDetail
    override fun getStubFactory(): StubElementFactory<KotlinPlaceHolderStubImpl<KtScriptInitializer>, KtScriptInitializer> =
        stubFactory
}
