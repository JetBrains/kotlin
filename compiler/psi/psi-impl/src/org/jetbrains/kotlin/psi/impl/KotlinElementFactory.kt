/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.impl

import org.jetbrains.kotlin.psi.KtCompanionBlock
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtUnionType
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub

/**
 * Creates stub-based PSI elements whose implementations are internal to the PSI.
 */
@KtImplementationDetail
interface KotlinElementFactory {
    /**
     * Creates a [KtCompanionBlock] from its stub.
     */
    fun createCompanionBlock(stub: KotlinPlaceHolderStub<KtCompanionBlock>): KtCompanionBlock

    /**
     * Creates a [KtUnionType] from its stub.
     */
    fun createUnionType(stub: KotlinPlaceHolderStub<KtUnionType>): KtUnionType
}

internal class KotlinElementFactoryImpl : KotlinElementFactory {
    override fun createCompanionBlock(stub: KotlinPlaceHolderStub<KtCompanionBlock>): KtCompanionBlock = KtCompanionBlockImpl(stub)

    override fun createUnionType(stub: KotlinPlaceHolderStub<KtUnionType>): KtUnionType = KtUnionTypeImpl(stub)
}
