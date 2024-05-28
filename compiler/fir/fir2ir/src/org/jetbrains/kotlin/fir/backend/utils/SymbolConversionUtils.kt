/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.utils

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.utils.UseSiteKind.*
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.declarations.isJavaOrEnhancement
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.FirSimpleSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.resolve.findClassRepresentation
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.impl.originalConstructorIfTypeAlias
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.unwrapFakeOverrides
import org.jetbrains.kotlin.fir.unwrapUseSiteSubstitutionOverrides
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

enum class ConversionTypeOrigin(val forSetter: Boolean) {
    DEFAULT(forSetter = false),
    SETTER(forSetter = true);
}

fun FirClassifierSymbol<*>.toIrSymbol(
    c: Fir2IrComponents,
    typeOrigin: ConversionTypeOrigin = ConversionTypeOrigin.DEFAULT,
    handleAnnotations: ((List<FirAnnotation>) -> Unit)? = null
): IrClassifierSymbol = with(c) {
    when (val symbol = this@toIrSymbol) {
        is FirTypeParameterSymbol -> {
            classifierStorage.getIrTypeParameterSymbol(symbol, typeOrigin)
        }

        is FirTypeAliasSymbol -> {
            handleAnnotations?.invoke(symbol.fir.expandedTypeRef.annotations)
            val coneClassLikeType = symbol.fir.expandedTypeRef.coneType as ConeClassLikeType
            coneClassLikeType.lookupTag.toSymbol(session)
                ?.toIrSymbol(c, typeOrigin, handleAnnotations)
                ?: classifierStorage.getIrClassForNotFoundClass(coneClassLikeType.lookupTag).symbol
        }

        is FirClassSymbol -> {
            classifierStorage.getIrClassSymbol(symbol)
        }

        else -> error("Unknown symbol: $symbol")
    }
}

fun FirReference.extractDeclarationSiteSymbol(c: Fir2IrComponents): FirCallableSymbol<*>? {
    if (this !is FirResolvedNamedReference) {
        return null
    }
    var symbol = resolvedSymbol as? FirCallableSymbol<*> ?: errorWithAttachment("Non-callable symbol got from call reference") {
        withEntry("symbol", resolvedSymbol.toString())
    }

    if (symbol.origin == FirDeclarationOrigin.SubstitutionOverride.CallSite) {
        symbol = symbol.fir.unwrapUseSiteSubstitutionOverrides<FirCallableDeclaration>().symbol
    }
    symbol = symbol.unwrapCallRepresentative(c)
    return symbol
}

private enum class UseSiteKind {
    GetCall,
    SetCall,
    Reference;
}

fun FirCallableSymbol<*>.toIrSymbolForCall(
    c: Fir2IrComponents,
    dispatchReceiver: FirExpression?,
    explicitReceiver: FirExpression?,
): IrSymbol? = c.toIrSymbol(
    this,
    dispatchReceiver,
    explicitReceiver,
    useSite = GetCall,
    isDelegate = false,
)

fun FirCallableSymbol<*>.toIrSymbolForSetCall(
    c: Fir2IrComponents,
    dispatchReceiver: FirExpression?,
    explicitReceiver: FirExpression?,
): IrSymbol? = c.toIrSymbol(
    this,
    dispatchReceiver,
    explicitReceiver,
    useSite = SetCall,
    isDelegate = false,
)

fun FirCallableSymbol<*>.toIrSymbolForCallableReference(
    c: Fir2IrComponents,
    dispatchReceiver: FirExpression?,
    lhs: FirExpression?,
    isDelegate: Boolean,
): IrSymbol? = c.toIrSymbol(
    this,
    dispatchReceiver,
    lhs,
    useSite = Reference,
    isDelegate = isDelegate,
)

private fun Fir2IrComponents.toIrSymbol(
    symbol: FirCallableSymbol<*>,
    dispatchReceiver: FirExpression?,
    // Note: in fact LHS for callable references and explicit receiver for normal qualified accesses
    explicitReceiver: FirExpression?,
    useSite: UseSiteKind,
    isDelegate: Boolean,
): IrSymbol? {
    val fakeOverrideOwnerLookupTag = when {
        // Static fake overrides
        symbol.isStatic -> {
            (dispatchReceiver as? FirResolvedQualifier)?.toLookupTag(session)
        }
        // Member fake override or bound callable reference
        dispatchReceiver != null -> {
            val callSiteDispatchReceiverType = when (dispatchReceiver) {
                is FirSmartCastExpression -> dispatchReceiver.smartcastTypeWithoutNullableNothing?.coneType ?: dispatchReceiver.resolvedType
                else -> dispatchReceiver.resolvedType
            }
            val declarationSiteDispatchReceiverType = symbol.dispatchReceiverType
            val type = if (callSiteDispatchReceiverType is ConeDynamicType && declarationSiteDispatchReceiverType != null) {
                declarationSiteDispatchReceiverType
            } else {
                callSiteDispatchReceiverType
            }
            type.findClassRepresentation(type, declarationStorage.session)
        }
        // Unbound callable reference to member (non-extension)
        useSite == Reference && symbol.fir.receiverParameter == null -> {
            (explicitReceiver as? FirResolvedQualifier)?.toLookupTag(session)
        }
        else -> null
    }
    return when (symbol) {
        is FirSimpleSyntheticPropertySymbol -> {
            when {
                isDelegate -> declarationStorage.getIrPropertySymbol(symbol)
                useSite == Reference -> declarationStorage.getIrPropertySymbol(symbol, fakeOverrideOwnerLookupTag)
                else -> {
                    val syntheticProperty = symbol.syntheticProperty
                    val delegateSymbol = if (useSite == GetCall) {
                        syntheticProperty.getter.delegate.symbol
                    } else {
                        syntheticProperty.setter?.delegate?.symbol ?: error("Written synthetic property must have a setter")
                    }
                    val unwrappedSymbol = delegateSymbol.unwrapCallRepresentative(this)
                    toIrSymbol(
                        unwrappedSymbol,
                        dispatchReceiver,
                        explicitReceiver = null,
                        useSite,
                        isDelegate = false,
                    )
                }
            }
        }
        is FirConstructorSymbol -> declarationStorage.getIrConstructorSymbol(symbol.fir.originalConstructorIfTypeAlias?.symbol ?: symbol)
        is FirFunctionSymbol<*> -> declarationStorage.getIrFunctionSymbol(symbol, fakeOverrideOwnerLookupTag)
        is FirPropertySymbol -> declarationStorage.getIrPropertySymbol(symbol, fakeOverrideOwnerLookupTag)
        is FirFieldSymbol -> {
            when (useSite) {
                Reference -> declarationStorage.getIrSymbolForField(symbol, fakeOverrideOwnerLookupTag)
                else -> {
                    val originalSymbol = symbol.unwrapFakeOverrides()
                    val ownerLookupTag = when {
                        originalSymbol.isJavaOrEnhancement -> null
                        else -> fakeOverrideOwnerLookupTag
                    }
                    val propertySymbol = declarationStorage.getIrSymbolForField(
                        originalSymbol,
                        fakeOverrideOwnerLookupTag = ownerLookupTag
                    ) as IrPropertySymbol
                    declarationStorage.findBackingFieldOfProperty(propertySymbol)
                }
            }
        }
        is FirBackingFieldSymbol -> declarationStorage.getIrBackingFieldSymbol(symbol)
        is FirDelegateFieldSymbol -> declarationStorage.getIrDelegateFieldSymbol(symbol)
        is FirVariableSymbol<*> -> declarationStorage.getIrValueSymbol(symbol)
        else -> null
    }
}

private fun FirResolvedQualifier.toLookupTag(session: FirSession): ConeClassLikeLookupTag? {
    return when (val symbol = symbol) {
        is FirClassSymbol -> symbol.toLookupTag()
        is FirTypeAliasSymbol -> symbol.fullyExpandedClass(session)?.toLookupTag()
        else -> null
    }
}
