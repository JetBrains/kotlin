/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.impl.FirMemberFunctionImpl
import org.jetbrains.kotlin.fir.java.types.FirJavaTypeRef
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name

class FirJavaMethod(
    session: FirSession,
    symbol: FirFunctionSymbol,
    name: Name,
    visibility: Visibility,
    modality: Modality?,
    returnTypeRef: FirJavaTypeRef
) : FirMemberFunctionImpl(
    session, null, symbol, name,
    visibility, modality,
    false, isActual = false,
    isOverride = false, // TODO: really it's unknown whether Java methods are overrides or not
    isOperator = true, // All Java methods with name that allows to use it in operator form are considered operators
    isInfix = false, isInline = false, isTailRec = false, isExternal = false, isSuspend = false,
    receiverTypeRef = null, returnTypeRef = returnTypeRef
)