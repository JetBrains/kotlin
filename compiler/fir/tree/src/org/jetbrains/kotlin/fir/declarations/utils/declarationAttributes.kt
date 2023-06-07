/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.utils

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyBackingField
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.impl.FirPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.name.Name

private object IsFromVarargKey : FirDeclarationDataKey()
private object IsReferredViaField : FirDeclarationDataKey()
private object IsFromPrimaryConstructor : FirDeclarationDataKey()
private object ComponentFunctionSymbolKey : FirDeclarationDataKey()
private object SourceElementKey : FirDeclarationDataKey()
private object ModuleNameKey : FirDeclarationDataKey()
private object DanglingTypeConstraintsKey : FirDeclarationDataKey()

var FirProperty.isFromVararg: Boolean? by FirDeclarationDataRegistry.data(IsFromVarargKey)
var FirProperty.isReferredViaField: Boolean? by FirDeclarationDataRegistry.data(IsReferredViaField)
var FirProperty.fromPrimaryConstructor: Boolean? by FirDeclarationDataRegistry.data(IsFromPrimaryConstructor)
var FirProperty.componentFunctionSymbol: FirNamedFunctionSymbol? by FirDeclarationDataRegistry.data(ComponentFunctionSymbolKey)
var FirClassLikeDeclaration.sourceElement: SourceElement? by FirDeclarationDataRegistry.data(SourceElementKey)
var FirRegularClass.moduleName: String? by FirDeclarationDataRegistry.data(ModuleNameKey)

val FirClassLikeSymbol<*>.sourceElement: SourceElement?
    get() = fir.sourceElement

val FirPropertySymbol.fromPrimaryConstructor: Boolean
    get() = fir.fromPrimaryConstructor ?: false

/**
 * Constraint without corresponding type argument
 */
data class DanglingTypeConstraint(val name: Name, val source: KtSourceElement)

var <T> T.danglingTypeConstraints: List<DanglingTypeConstraint>?
        where T : FirDeclaration, T : FirTypeParameterRefsOwner
        by FirDeclarationDataRegistry.data(DanglingTypeConstraintsKey)

// ----------------------------------- Utils -----------------------------------

val FirMemberDeclaration.containerSource: SourceElement?
    get() = when (this) {
        is FirCallableDeclaration -> containerSource
        is FirClassLikeDeclaration -> sourceElement
    }

val FirProperty.hasExplicitBackingField: Boolean
    get() = backingField != null && backingField !is FirDefaultPropertyBackingField

val FirPropertySymbol.hasExplicitBackingField: Boolean
    get() = fir.hasExplicitBackingField

fun FirProperty.getExplicitBackingField(): FirBackingField? {
    return if (hasExplicitBackingField) {
        backingField
    } else {
        null
    }
}

fun FirPropertySymbol.getExplicitBackingField(): FirBackingField? {
    return fir.getExplicitBackingField()
}

val FirProperty.canNarrowDownGetterType: Boolean
    get() {
        val backingFieldHasDifferentType = backingField != null && backingField?.returnTypeRef?.coneType != returnTypeRef.coneType
        return backingFieldHasDifferentType && getter is FirDefaultPropertyGetter
    }

val FirPropertySymbol.canNarrowDownGetterType: Boolean
    get() = fir.canNarrowDownGetterType

// See [BindingContext.BACKING_FIELD_REQUIRED]
val FirProperty.hasBackingField: Boolean
    get() {
        if (isAbstract) return false
        if (delegate != null) return false
        if (hasExplicitBackingField) return true
        if (symbol is FirSyntheticPropertySymbol) return false
        if (isStatic) return false // For Enum.entries
        when (origin) {
            is FirDeclarationOrigin.SubstitutionOverride -> return false
            FirDeclarationOrigin.IntersectionOverride -> return false
            FirDeclarationOrigin.Delegated -> return false
            else -> {
                val getter = getter ?: return true
                if (isVar && setter == null) return true
                if (setter?.hasBody == false && setter?.isAbstract == false) return true
                if (!getter.hasBody && !getter.isAbstract) return true

                return isReferredViaField == true
            }
        }
    }

val FirPropertySymbol.hasBackingField: Boolean
    get() {
        lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
        return fir.hasBackingField
    }

fun FirDeclaration.getDanglingTypeConstraintsOrEmpty(): List<DanglingTypeConstraint> {
    return when (this) {
        is FirRegularClass -> danglingTypeConstraints
        is FirSimpleFunction -> danglingTypeConstraints
        is FirProperty -> danglingTypeConstraints
        else -> null
    } ?: emptyList()
}

val FirPropertySymbol.correspondingValueParameterFromPrimaryConstructor: FirValueParameterSymbol?
    get() = fir.correspondingValueParameterFromPrimaryConstructor

val FirProperty.correspondingValueParameterFromPrimaryConstructor: FirValueParameterSymbol?
    get() {
        if (fromPrimaryConstructor != true) return null
        val initializer = initializer as? FirPropertyAccessExpression ?: return null
        val reference = initializer.calleeReference as? FirPropertyFromParameterResolvedNamedReference ?: return null
        return reference.resolvedSymbol as? FirValueParameterSymbol
    }
