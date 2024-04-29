/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.utils

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.FirSimpleSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.resolve.findClassRepresentation
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.impl.originalConstructorIfTypeAlias
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.unwrapUseSiteSubstitutionOverrides
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

enum class ConversionTypeOrigin(val forSetter: Boolean) {
    DEFAULT(forSetter = false),
    SETTER(forSetter = true);
}

fun FirClassifierSymbol<*>.toSymbol(
    c: Fir2IrComponents,
    typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT,
    handleAnnotations: ((List<FirAnnotation>) -> Unit)? = null
): IrClassifierSymbol = with(c) {
    val symbol = this@toSymbol
    when (symbol) {
        is FirTypeParameterSymbol -> {
            classifierStorage.getIrTypeParameterSymbol(symbol, typeOrigin)
        }

        is FirTypeAliasSymbol -> {
            handleAnnotations?.invoke(symbol.fir.expandedTypeRef.annotations)
            val coneClassLikeType = symbol.fir.expandedTypeRef.coneType as ConeClassLikeType
            coneClassLikeType.lookupTag.toSymbol(session)
                ?.toSymbol(c, typeOrigin, handleAnnotations)
                ?: classifiersGenerator.createIrClassForNotFoundClass(coneClassLikeType.lookupTag).symbol
        }

        is FirClassSymbol -> {
            classifierStorage.getIrClassSymbol(symbol)
        }

        else -> error("Unknown symbol: $symbol")
    }
}

private fun FirBasedSymbol<*>.toSymbolForCall(
    c: Fir2IrComponents,
    dispatchReceiver: FirExpression?,
    preferGetter: Boolean,
    explicitReceiver: FirExpression? = null,
    isDelegate: Boolean = false,
    isReference: Boolean = false
): IrSymbol? = when (this) {
    is FirCallableSymbol<*> -> toSymbolForCall(
        c,
        dispatchReceiver,
        preferGetter,
        explicitReceiver,
        isDelegate,
        isReference
    )

    is FirClassifierSymbol<*> -> toSymbol(c)
    else -> error("Unknown symbol: $this")
}

fun FirReference.extractSymbolForCall(c: Fir2IrComponents): FirBasedSymbol<*>? {
    if (this !is FirResolvedNamedReference) {
        return null
    }
    var symbol = resolvedSymbol

    if (symbol is FirCallableSymbol<*>) {
        if (symbol.origin == FirDeclarationOrigin.SubstitutionOverride.CallSite) {
            symbol = symbol.fir.unwrapUseSiteSubstitutionOverrides<FirCallableDeclaration>().symbol
        }
        @Suppress("USELESS_CAST") // K2 warning suppression, TODO: KT-62472
        symbol = (symbol as FirCallableSymbol<*>).unwrapCallRepresentative(c)
    }
    return symbol
}

@OptIn(ExperimentalContracts::class)
fun FirReference.toSymbolForCall(
    c: Fir2IrComponents,
    dispatchReceiver: FirExpression?,
    explicitReceiver: FirExpression?,
    preferGetter: Boolean = true,
    isDelegate: Boolean = false,
    isReference: Boolean = false,
): IrSymbol? {
    contract {
        returnsNotNull() implies (this@toSymbolForCall is FirResolvedNamedReference)
    }

    return extractSymbolForCall(c)?.toSymbolForCall(
        c,
        dispatchReceiver,
        preferGetter,
        explicitReceiver,
        isDelegate,
        isReference
    )
}

private fun FirResolvedQualifier.toLookupTag(session: FirSession): ConeClassLikeLookupTag? =
    when (val symbol = symbol) {
        is FirClassSymbol -> symbol.toLookupTag()
        is FirTypeAliasSymbol -> symbol.fullyExpandedClass(session)?.toLookupTag()
        else -> null
    }

fun FirCallableSymbol<*>.toSymbolForCall(
    c: Fir2IrComponents,
    dispatchReceiver: FirExpression?,
    preferGetter: Boolean = true,
    // Note: in fact LHS for callable references and explicit receiver for normal qualified accesses
    explicitReceiver: FirExpression? = null,
    isDelegate: Boolean = false,
    isReference: Boolean = false
): IrSymbol? = with(c) {
    val fakeOverrideOwnerLookupTag = when {
        // Static fake overrides
        isStatic -> {
            (dispatchReceiver as? FirResolvedQualifier)?.toLookupTag(session)
        }
        // Member fake override or bound callable reference
        dispatchReceiver != null -> {
            val callSiteDispatchReceiverType = when (dispatchReceiver) {
                is FirSmartCastExpression -> dispatchReceiver.smartcastTypeWithoutNullableNothing?.coneType ?: dispatchReceiver.resolvedType
                else -> dispatchReceiver.resolvedType
            }
            val declarationSiteDispatchReceiverType = dispatchReceiverType
            val type = if (callSiteDispatchReceiverType is ConeDynamicType && declarationSiteDispatchReceiverType != null) {
                declarationSiteDispatchReceiverType
            } else {
                callSiteDispatchReceiverType
            }
            type.findClassRepresentation(type, declarationStorage.session)
        }
        // Unbound callable reference to member (non-extension)
        isReference && fir.receiverParameter == null -> {
            (explicitReceiver as? FirResolvedQualifier)?.toLookupTag(session)
        }
        else -> null
    }
    return when (val symbol =  this@toSymbolForCall) {
        is FirSimpleSyntheticPropertySymbol -> {
            if (isDelegate) {
                declarationStorage.getIrPropertySymbol(symbol)
            } else {
                (fir as? FirSyntheticProperty)?.let { syntheticProperty ->
                    if (isReference) {
                        declarationStorage.getIrPropertySymbol(symbol, fakeOverrideOwnerLookupTag)
                    } else {
                        val delegateSymbol = if (preferGetter) {
                            syntheticProperty.getter.delegate.symbol
                        } else {
                            syntheticProperty.setter?.delegate?.symbol
                                ?: throw AssertionError("Written synthetic property must have a setter")
                        }
                        delegateSymbol.unwrapCallRepresentative(c)
                            .toSymbolForCall(c, dispatchReceiver, preferGetter, isDelegate = false)
                    }
                } ?: declarationStorage.getIrPropertySymbol(symbol)
            }
        }
        is FirConstructorSymbol -> declarationStorage.getIrConstructorSymbol(symbol.fir.originalConstructorIfTypeAlias?.symbol ?: symbol)
        is FirFunctionSymbol<*> -> declarationStorage.getIrFunctionSymbol(symbol, fakeOverrideOwnerLookupTag)
        is FirPropertySymbol -> declarationStorage.getIrPropertySymbol(symbol, fakeOverrideOwnerLookupTag)
        is FirFieldSymbol -> declarationStorage.getOrCreateIrField(symbol, fakeOverrideOwnerLookupTag).symbol
        is FirBackingFieldSymbol -> declarationStorage.getIrBackingFieldSymbol(symbol)
        is FirDelegateFieldSymbol -> declarationStorage.getIrDelegateFieldSymbol(symbol)
        is FirVariableSymbol<*> -> declarationStorage.getIrValueSymbol(symbol)
        else -> null
    }
}
