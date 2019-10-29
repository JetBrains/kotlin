/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef

sealed class ResolutionMode {
    object ContextDependent : ResolutionMode()
    object ContextIndependent : ResolutionMode()
    // TODO: it's better not to use WithExpectedType(FirImplicitTypeRef)
    class WithExpectedType(val expectedTypeRef: FirTypeRef) : ResolutionMode()

    class WithStatus(val status: FirDeclarationStatus) : ResolutionMode()

    class LambdaResolution(val expectedReturnTypeRef: FirResolvedTypeRef?) : ResolutionMode()
}

fun withExpectedType(expectedTypeRef: FirTypeRef?): ResolutionMode =
    expectedTypeRef?.let { ResolutionMode.WithExpectedType(it) } ?: ResolutionMode.ContextDependent

fun FirDeclarationStatus.mode(): ResolutionMode =
    ResolutionMode.WithStatus(this)