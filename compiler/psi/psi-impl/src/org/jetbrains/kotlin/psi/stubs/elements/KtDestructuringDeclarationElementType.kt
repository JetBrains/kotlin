/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental

package org.jetbrains.kotlin.psi.stubs.elements

import com.intellij.psi.stubs.StubElementFactory
import com.intellij.psi.stubs.StubSerializer
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.stubs.KotlinDestructuringDeclarationStub
import org.jetbrains.kotlin.psi.stubs.factory.KotlinDestructuringDeclarationStubFactory
import org.jetbrains.kotlin.psi.stubs.factory.KotlinDestructuringDeclarationStubSerializer
import org.jetbrains.kotlin.psi.stubs.impl.KotlinDestructuringDeclarationStubImpl

internal object KtDestructuringDeclarationElementType :
    KtStubElementType<KotlinDestructuringDeclarationStubImpl, KtDestructuringDeclaration>(
        /* debugName = */ "DESTRUCTURING_DECLARATION",
        /* psiClass = */ KtDestructuringDeclaration::class.java,
        /* stubClass = */ KotlinDestructuringDeclarationStub::class.java,
    ) {
    @KtImplementationDetail
    override fun getStubFactory(): StubElementFactory<KotlinDestructuringDeclarationStubImpl, KtDestructuringDeclaration> =
        KotlinDestructuringDeclarationStubFactory

    @KtImplementationDetail
    override fun getStubSerializer(): StubSerializer<KotlinDestructuringDeclarationStubImpl> =
        KotlinDestructuringDeclarationStubSerializer
}
