/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.declarations

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirField
import org.jetbrains.kotlin.fir.declarations.impl.FirAbstractCallableMember
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.java.types.FirJavaTypeRef
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.name.Name

class FirJavaField(
    session: FirSession,
    override val symbol: FirPropertySymbol,
    name: Name,
    visibility: Visibility,
    modality: Modality?,
    returnTypeRef: FirTypeRef,
    override val isVar: Boolean,
    isStatic: Boolean
) : FirAbstractCallableMember(
    session, psi = null, name = name,
    visibility = visibility, modality = modality,
    isExpect = false, isActual = false, isOverride = false,
    receiverTypeRef = null, returnTypeRef = returnTypeRef
), FirField {
    init {
        symbol.bind(this)
    }

    override val delegate: FirExpression?
        get() = null

    override val initializer: FirExpression?
        get() = null

    init {
        status.isStatic = isStatic
    }
}