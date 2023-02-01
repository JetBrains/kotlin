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
inline val FirCallableSymbol<*>.isInner: Boolean get() = rawStatus.isInner
inline val FirCallableSymbol<*>.isStatic: Boolean get() = rawStatus.isStatic
inline val FirCallableSymbol<*>.isOverride: Boolean get() = rawStatus.isOverride
inline val FirCallableSymbol<*>.isOperator: Boolean get() = resolvedStatus.isOperator
inline val FirCallableSymbol<*>.isInfix: Boolean get() = resolvedStatus.isInfix
inline val FirCallableSymbol<*>.isInline: Boolean get() = rawStatus.isInline
inline val FirCallableSymbol<*>.isTailRec: Boolean get() = rawStatus.isTailRec
inline val FirCallableSymbol<*>.isExternal: Boolean get() = rawStatus.isExternal
inline val FirCallableSymbol<*>.isSuspend: Boolean get() = rawStatus.isSuspend
inline val FirCallableSymbol<*>.isConst: Boolean get() = rawStatus.isConst
inline val FirCallableSymbol<*>.isLateInit: Boolean get() = rawStatus.isLateInit
inline val FirCallableSymbol<*>.isFromSealedClass: Boolean get() = rawStatus.isFromSealedClass
inline val FirCallableSymbol<*>.isFromEnumClass: Boolean get() = rawStatus.isFromEnumClass
inline val FirCallableSymbol<*>.isFun: Boolean get() = rawStatus.isFun

// ---------------------- class like with status ----------------------

inline val FirClassLikeSymbol<*>.modality: Modality? get() = resolvedStatus.modality
inline val FirClassLikeSymbol<*>.isAbstract: Boolean get() = resolvedStatus.modality == Modality.ABSTRACT
inline val FirClassLikeSymbol<*>.isOpen: Boolean get() = resolvedStatus.modality == Modality.OPEN
inline val FirClassLikeSymbol<*>.isFinal: Boolean
    get() {
        // member with unspecified modality is final
        val modality = resolvedStatus.modality ?: return true
        return modality == Modality.FINAL
    }

inline val FirClassLikeSymbol<*>.visibility: Visibility get() = resolvedStatus.visibility
inline val FirClassLikeSymbol<*>.effectiveVisibility: EffectiveVisibility get() = resolvedStatus.effectiveVisibility

inline val FirClassLikeSymbol<*>.isActual: Boolean get() = resolvedStatus.isActual
inline val FirClassLikeSymbol<*>.isExpect: Boolean get() = resolvedStatus.isExpect
inline val FirClassLikeSymbol<*>.isInner: Boolean get() = rawStatus.isInner
inline val FirClassLikeSymbol<*>.isStatic: Boolean get() = rawStatus.isStatic
inline val FirClassLikeSymbol<*>.isInline: Boolean get() = rawStatus.isInline
inline val FirClassLikeSymbol<*>.isExternal: Boolean get() = rawStatus.isExternal
inline val FirClassLikeSymbol<*>.isFromSealedClass: Boolean get() = rawStatus.isFromSealedClass
inline val FirClassLikeSymbol<*>.isFromEnumClass: Boolean get() = rawStatus.isFromEnumClass
inline val FirClassLikeSymbol<*>.isFun: Boolean get() = rawStatus.isFun
inline val FirClassLikeSymbol<*>.isCompanion: Boolean get() = rawStatus.isCompanion
inline val FirClassLikeSymbol<*>.isData: Boolean get() = rawStatus.isData
inline val FirClassLikeSymbol<*>.isSealed: Boolean get() = resolvedStatus.modality == Modality.SEALED

inline val FirRegularClassSymbol.canHaveAbstractDeclaration: Boolean
    get() = isAbstract || isSealed || isEnumClass

// ---------------------- common classes ----------------------

inline val FirClassLikeSymbol<*>.isLocal: Boolean get() = classId.isLocal

inline val FirClassSymbol<*>.isLocalClassOrAnonymousObject: Boolean
    get() = classId.isLocal || this is FirAnonymousObjectSymbol


inline val FirClassSymbol<*>.isClass: Boolean
    get() = classKind.isClass

inline val FirClassSymbol<*>.isInterface: Boolean
    get() = classKind.isInterface

inline val FirClassSymbol<*>.isEnumClass: Boolean
    get() = classKind.isEnumClass

// ---------------------- specific callables ----------------------

inline val FirPropertyAccessorSymbol.allowsToHaveFakeOverride: Boolean get() = visibility.allowsToHaveFakeOverride

inline val FirPropertySymbol.allowsToHaveFakeOverride: Boolean get() = visibility.allowsToHaveFakeOverride

inline val FirNamedFunctionSymbol.isLocal: Boolean get() = visibility == Visibilities.Local
