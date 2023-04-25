/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.signaturer.FirMangler
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.ir.util.IdSignature

interface Fir2IrSignatureComposer {
    val mangler: FirMangler
    fun composeSignature(
        declaration: FirDeclaration,
        containingClass: ConeClassLikeLookupTag? = null,
        forceTopLevelPrivate: Boolean = false,
        forceExpect: Boolean = false,
    ): IdSignature?

    fun composeAccessorSignature(
        property: FirProperty,
        isSetter: Boolean,
        containingClass: ConeClassLikeLookupTag? = null,
        forceTopLevelPrivate: Boolean = false
    ): IdSignature?

    fun composeTypeParameterSignature(typeParameter: FirTypeParameter, index: Int, containerSignature: IdSignature?): IdSignature?
    fun withFileSignature(sig: IdSignature.FileSignature, body: () -> Unit)
}
