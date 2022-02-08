/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.utils

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.symbols.impl.*

// ---------------------- callables with status ----------------------

inline val FirCallableSymbol<*>.modality: Modality? get() = resolvedStatus.modality
inline val FirCallableSymbol<*>.modalityOrFinal: Modality get() = modality ?: Modality.FINAL
inline val FirCallableSymbol<*>.isAbstract: Boolean get() = resolvedStatus.modality == Modality.ABSTRACT
inline val FirCallableSymbol<*>.isOpen: Boolean get() = resolvedStatus.modality == Modality.OPEN
inline val FirCallableSymbol<*>.isFinal: Boolean
    get() {
        // member with unspecified modality is final
        val modality = resolvedStatus.modality ?: return true
        return modality == Modality.FINAL
    }

inline val FirCallableSymbol<*>.visibility: Visibility get() = resolvedStatus.visibility
inline val FirCallableSymbol<*>.effectiveVisibility: EffectiveVisibility get() = resolvedStatus.effectiveVisibility

inline val FirCallableSymbol<*>.allowsToHaveFakeOverride: Boolean
    get() = !Visibilities.isPrivate(visibility) && visibility != Visibilities.InvisibleFake

inline val FirCallableSymbol<*>.isActual: Boolean get() = resolvedStatus.isActual
inline val FirCallableSymbol<*>.isExpect: Boolean get() = resolvedStatus.isExpect
inline val FirCallableSymbol<*>.isInner: Boolean get() = resolvedStatus.isInner
inline val FirCallableSymbol<*>.isStatic: Boolean get() = resolvedStatus.isStatic
inline val FirCallableSymbol<*>.isOverride: Boolean get() = resolvedStatus.isOverride
inline val FirCallableSymbol<*>.isOperator: Boolean get() = resolvedStatus.isOperator
inline val FirCallableSymbol<*>.isInfix: Boolean get() = resolvedStatus.isInfix
inline val FirCallableSymbol<*>.isInline: Boolean get() = resolvedStatus.isInline
inline val FirCallableSymbol<*>.isTailRec: Boolean get() = resolvedStatus.isTailRec
inline val FirCallableSymbol<*>.isExternal: Boolean get() = resolvedStatus.isExternal
inline val FirCallableSymbol<*>.isSuspend: Boolean get() = resolvedStatus.isSuspend
inline val FirCallableSymbol<*>.isConst: Boolean get() = resolvedStatus.isConst
inline val FirCallableSymbol<*>.isLateInit: Boolean get() = resolvedStatus.isLateInit
inline val FirCallableSymbol<*>.isFromSealedClass: Boolean get() = resolvedStatus.isFromSealedClass
inline val FirCallableSymbol<*>.isFromEnumClass: Boolean get() = resolvedStatus.isFromEnumClass
inline val FirCallableSymbol<*>.isFun: Boolean get() = resolvedStatus.isFun

// ---------------------- regular with status ----------------------

inline val FirClassSymbol<*>.modality: Modality? get() = resolvedStatus.modality
inline val FirClassSymbol<*>.isAbstract: Boolean get() = resolvedStatus.modality == Modality.ABSTRACT
inline val FirClassSymbol<*>.isOpen: Boolean get() = resolvedStatus.modality == Modality.OPEN
inline val FirClassSymbol<*>.isFinal: Boolean
    get() {
        // member with unspecified modality is final
        val modality = resolvedStatus.modality ?: return true
        return modality == Modality.FINAL
    }

inline val FirClassSymbol<*>.visibility: Visibility get() = resolvedStatus.visibility
inline val FirClassSymbol<*>.effectiveVisibility: EffectiveVisibility get() = resolvedStatus.effectiveVisibility

inline val FirClassSymbol<*>.isActual: Boolean get() = resolvedStatus.isActual
inline val FirClassSymbol<*>.isExpect: Boolean get() = resolvedStatus.isExpect
inline val FirClassSymbol<*>.isInner: Boolean get() = resolvedStatus.isInner
inline val FirClassSymbol<*>.isStatic: Boolean get() = resolvedStatus.isStatic
inline val FirClassSymbol<*>.isInline: Boolean get() = resolvedStatus.isInline
inline val FirClassSymbol<*>.isExternal: Boolean get() = resolvedStatus.isExternal
inline val FirClassSymbol<*>.isFromSealedClass: Boolean get() = resolvedStatus.isFromSealedClass
inline val FirClassSymbol<*>.isFromEnumClass: Boolean get() = resolvedStatus.isFromEnumClass
inline val FirClassSymbol<*>.isFun: Boolean get() = resolvedStatus.isFun
inline val FirClassSymbol<*>.isCompanion: Boolean get() = resolvedStatus.isCompanion
inline val FirClassSymbol<*>.isData: Boolean get() = resolvedStatus.isData
inline val FirClassSymbol<*>.isSealed: Boolean get() = resolvedStatus.modality == Modality.SEALED

inline val FirRegularClassSymbol.canHaveAbstractDeclaration: Boolean
    get() = isAbstract || isSealed || isEnumClass

// ---------------------- type aliases with status ----------------------

inline val FirTypeAliasSymbol.modality: Modality? get() = resolvedStatus.modality
inline val FirTypeAliasSymbol.isAbstract: Boolean get() = resolvedStatus.modality == Modality.ABSTRACT
inline val FirTypeAliasSymbol.isOpen: Boolean get() = resolvedStatus.modality == Modality.OPEN
inline val FirTypeAliasSymbol.isFinal: Boolean
    get() {
        // member with unspecified modality is final
        val modality = resolvedStatus.modality ?: return true
        return modality == Modality.FINAL
    }

inline val FirTypeAliasSymbol.visibility: Visibility get() = resolvedStatus.visibility
inline val FirTypeAliasSymbol.effectiveVisibility: EffectiveVisibility
    get() = resolvedStatus.effectiveVisibility

inline val FirTypeAliasSymbol.isActual: Boolean get() = resolvedStatus.isActual
inline val FirTypeAliasSymbol.isExpect: Boolean get() = resolvedStatus.isExpect
inline val FirTypeAliasSymbol.isInner: Boolean get() = resolvedStatus.isInner
inline val FirTypeAliasSymbol.isStatic: Boolean get() = resolvedStatus.isStatic
inline val FirTypeAliasSymbol.isInline: Boolean get() = resolvedStatus.isInline
inline val FirTypeAliasSymbol.isExternal: Boolean get() = resolvedStatus.isExternal
inline val FirTypeAliasSymbol.isFromSealedClass: Boolean get() = resolvedStatus.isFromSealedClass
inline val FirTypeAliasSymbol.isFromEnumClass: Boolean get() = resolvedStatus.isFromEnumClass
inline val FirTypeAliasSymbol.isFun: Boolean get() = resolvedStatus.isFun

// ---------------------- common classes ----------------------

inline val FirClassLikeSymbol<*>.isLocal: Boolean get() = classId.isLocal

inline val FirClassSymbol<*>.isLocalClassOrAnonymousObject: Boolean
    get() = classId.isLocal || this is FirAnonymousObjectSymbol

inline val FirClassLikeSymbol<*>.isExpect: Boolean
    get() = when (this) {
        is FirRegularClassSymbol -> isExpect
        is FirTypeAliasSymbol -> isExpect
        else -> false
    }

inline val FirClassLikeSymbol<*>.isActual: Boolean
    get() = when (this) {
        is FirRegularClassSymbol -> isActual
        is FirTypeAliasSymbol -> isActual
        else -> false
    }

inline val FirClassSymbol<*>.isInterface: Boolean
    get() = classKind.isInterface

inline val FirClassSymbol<*>.isEnumClass: Boolean
    get() = classKind.isEnumClass

// ---------------------- specific callables ----------------------

inline val FirPropertyAccessorSymbol.allowsToHaveFakeOverride: Boolean get() = visibility.allowsToHaveFakeOverride

inline val FirPropertySymbol.allowsToHaveFakeOverride: Boolean get() = visibility.allowsToHaveFakeOverride

inline val FirNamedFunctionSymbol.isLocal: Boolean get() = visibility == Visibilities.Local
