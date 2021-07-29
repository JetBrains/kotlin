/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.fir

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.utils.fromPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isFromVararg
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.expressions.builder.buildQualifiedAccessExpression
import org.jetbrains.kotlin.fir.lightTree.fir.modifier.Modifier
import org.jetbrains.kotlin.fir.references.builder.buildPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.FirImplicitTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildErrorTypeRef
import org.jetbrains.kotlin.name.CallableId

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

    fun toFirProperty(
        moduleData: FirModuleData,
        callableId: CallableId,
        isExpect: Boolean,
        currentDispatchReceiver: ConeClassLikeType?
    ): FirProperty {
        val name = this.firValueParameter.name
        var type = this.firValueParameter.returnTypeRef
        if (type is FirImplicitTypeRef) {
            type = buildErrorTypeRef { diagnostic = ConeSimpleDiagnostic("Incomplete code", DiagnosticKind.Syntax) }
        }

        return buildProperty {
            val propertySource = firValueParameter.source?.fakeElement(FirFakeSourceElementKind.PropertyFromParameter)
            source = propertySource
            this.moduleData = moduleData
            origin = FirDeclarationOrigin.Source
            returnTypeRef = type.copyWithNewSourceKind(FirFakeSourceElementKind.PropertyFromParameter)
            this.name = name
            initializer = buildQualifiedAccessExpression {
                source = propertySource
                calleeReference = buildPropertyFromParameterResolvedNamedReference {
                    source = propertySource
                    this.name = name
                    resolvedSymbol = this@ValueParameter.firValueParameter.symbol
                    source = propertySource
                }
            }
            isVar = this@ValueParameter.isVar
            symbol = FirPropertySymbol(callableId)
            dispatchReceiverType = currentDispatchReceiver
            isLocal = false
            status = FirDeclarationStatusImpl(modifiers.getVisibility(), modifiers.getModality(isClassOrObject = false)).apply {
                this.isExpect = isExpect
                isActual = modifiers.hasActual()
                isOverride = modifiers.hasOverride()
                isConst = modifiers.hasConst()
                isLateInit = false
            }
            annotations += this@ValueParameter.firValueParameter.annotations
            val defaultAccessorSource = propertySource?.fakeElement(FirFakeSourceElementKind.DefaultAccessor)
            getter = FirDefaultPropertyGetter(
                defaultAccessorSource,
                moduleData,
                FirDeclarationOrigin.Source,
                type.copyWithNewSourceKind(FirFakeSourceElementKind.DefaultAccessor),
                modifiers.getVisibility()
            )
            setter = if (this.isVar) FirDefaultPropertySetter(
                defaultAccessorSource,
                moduleData,
                FirDeclarationOrigin.Source,
                type.copyWithNewSourceKind(FirFakeSourceElementKind.DefaultAccessor),
                modifiers.getVisibility()
            ) else null
        }.apply {
            if (firValueParameter.isVararg) {
                this.isFromVararg = true
            }
            this.fromPrimaryConstructor = true
        }
    }
}
