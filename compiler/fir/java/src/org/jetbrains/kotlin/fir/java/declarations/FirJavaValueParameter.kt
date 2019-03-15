/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.impl.FirValueParameterImpl
import org.jetbrains.kotlin.fir.java.types.FirJavaTypeRef
import org.jetbrains.kotlin.name.Name

class FirJavaValueParameter(
    session: FirSession,
    name: Name,
    returnTypeRef: FirJavaTypeRef,
    isVararg: Boolean
) : FirValueParameterImpl(
    session, psi = null, name = name, returnTypeRef = returnTypeRef,
    defaultValue = null, isCrossinline = false, isNoinline = false, isVararg = isVararg
)