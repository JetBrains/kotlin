/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.impl.FirPropertyImpl
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.FirSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.impl.FirQualifiedAccessExpressionImpl
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.Modifier
import org.jetbrains.kotlin.fir.references.impl.FirPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirErrorTypeRefImpl

class ValueParameter(
    private val isVal: Boolean,
    private val isVar: Boolean,
    private val modifiers: Modifier,
    val firValueParameter: FirValueParameter,
    val destructuringDeclaration: DestructuringDeclaration? = null
) {
    fun hasValOrVar(): Boolean {
        return isVal || isVar
    }

    fun toFirProperty(session: FirSession, callableId: CallableId): FirProperty {
        val name = this.firValueParameter.name
        var type = this.firValueParameter.returnTypeRef
        if (type is FirImplicitTypeRef) {
            type = FirErrorTypeRefImpl(null, FirSimpleDiagnostic("Incomplete code", DiagnosticKind.Syntax))
        }

        val status = FirDeclarationStatusImpl(modifiers.getVisibility(), modifiers.getModality()).apply {
            isExpect = modifiers.hasExpect()
            isActual = modifiers.hasActual()
            isOverride = modifiers.hasOverride()
            isConst = false
            isLateInit = false
        }

        return FirPropertyImpl(
            null,
            session,
            type,
            null,
            name,
            FirQualifiedAccessExpressionImpl(null).apply {
                calleeReference = FirPropertyFromParameterResolvedNamedReference(
                    null, name, this@ValueParameter.firValueParameter.symbol
                )
            },
            null,
            this.isVar,
            FirPropertySymbol(callableId),
            false,
            status
        ).apply {
            annotations += this@ValueParameter.firValueParameter.annotations
            getter = FirDefaultPropertyGetter(null, session, type, modifiers.getVisibility())
            setter = if (this.isVar) FirDefaultPropertySetter(null, session, type, modifiers.getVisibility()) else null
        }
    }
}
