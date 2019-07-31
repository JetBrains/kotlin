/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirAbstractCallableMember
import org.jetbrains.kotlin.fir.declarations.impl.FirConstructorImpl.Companion.NAME
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef

class FirJavaConstructor(
    session: FirSession,
    override val symbol: FirConstructorSymbol,
    visibility: Visibility,
    override val isPrimary: Boolean,
    delegatedSelfTypeRef: FirTypeRef
) : FirAbstractCallableMember<FirConstructor>(
    session,
    psi = null,
    name = NAME,
    visibility = visibility,
    modality = Modality.FINAL,
    isExpect = false,
    isActual = false,
    isOverride = false,
    receiverTypeRef = null,
    returnTypeRef = delegatedSelfTypeRef
), FirConstructor {

    init {
        symbol.bind(this)
        resolvePhase = FirResolvePhase.DECLARATIONS
    }

    override val delegatedConstructor: FirDelegatedConstructorCall?
        get() = null

    override val body: FirBlock?
        get() = null

    override val valueParameters = mutableListOf<FirValueParameter>()
}