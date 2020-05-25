/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.isFromVararg
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.expressions.builder.buildQualifiedAccessExpression
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.Modifier
import org.jetbrains.kotlin.fir.references.builder.buildPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef

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
            type = buildErrorTypeRef { diagnostic = ConeSimpleDiagnostic("Incomplete code", DiagnosticKind.Syntax) }
        }

        return buildProperty {
            source = firValueParameter.source
            this.session = session
            origin = FirDeclarationOrigin.Source
            returnTypeRef = type
            this.name = name
            initializer = buildQualifiedAccessExpression {
                calleeReference = buildPropertyFromParameterResolvedNamedReference {
                    this.name =  name
                    resolvedSymbol = this@ValueParameter.firValueParameter.symbol
                }
            }
            isVar = this@ValueParameter.isVar
            symbol = FirPropertySymbol(callableId)
            isLocal = false
            status = FirDeclarationStatusImpl(modifiers.getVisibility(), modifiers.getModality()).apply {
                isExpect = modifiers.hasExpect()
                isActual = modifiers.hasActual()
                isOverride = modifiers.hasOverride()
                isConst = false
                isLateInit = false
            }
            annotations += this@ValueParameter.firValueParameter.annotations
            getter = FirDefaultPropertyGetter(null, session, FirDeclarationOrigin.Source, type, modifiers.getVisibility())
            setter = if (this.isVar) FirDefaultPropertySetter(null, session, FirDeclarationOrigin.Source, type, modifiers.getVisibility()) else null
        }.apply {
            this.isFromVararg = firValueParameter.isVararg
        }
    }
}
