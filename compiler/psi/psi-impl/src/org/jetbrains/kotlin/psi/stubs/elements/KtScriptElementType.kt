/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElementFactory
import com.intellij.psi.stubs.StubSerializer
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.stubs.KotlinScriptStub
import org.jetbrains.kotlin.psi.stubs.factory.KotlinScriptStubFactory
import org.jetbrains.kotlin.psi.stubs.factory.KotlinScriptStubSerializer
import org.jetbrains.kotlin.psi.stubs.impl.KotlinScriptStubImpl

class KtScriptElementType(debugName: String) : KtStubElementType<KotlinScriptStubImpl, KtScript>(
    debugName,
    KtScript::class.java,
    KotlinScriptStub::class.java,
) {
    override fun getStubFactory(): StubElementFactory<KotlinScriptStubImpl, KtScript> = KotlinScriptStubFactory

    override fun getStubSerializer(): StubSerializer<KotlinScriptStubImpl> = KotlinScriptStubSerializer
}
