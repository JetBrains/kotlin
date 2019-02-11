/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.impl

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef

class FirPrimaryConstructorImpl(
    session: FirSession,
    psi: PsiElement?,
    symbol: FirFunctionSymbol,
    visibility: Visibility,
    isExpect: Boolean,
    isActual: Boolean,
    delegatedSelfTypeRef: FirTypeRef,
    delegatedConstructor: FirDelegatedConstructorCall?
) : FirConstructorImpl(session, psi, symbol, visibility, isExpect, isActual, delegatedSelfTypeRef, delegatedConstructor)