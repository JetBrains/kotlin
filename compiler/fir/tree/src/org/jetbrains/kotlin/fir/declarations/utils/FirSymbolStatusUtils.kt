/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.utils

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol

// ---------------------- callables with status ----------------------

inline val FirCallableSymbol<*>.modality: Modality get() = resolvedStatus.modality
inline val FirCallableSymbol<*>.isAbstract: Boolean get() = resolvedStatus.modality == Modality.ABSTRACT
inline val FirCallableSymbol<*>.isOpen: Boolean get() = resolvedStatus.modality == Modality.OPEN
inline val FirCallableSymbol<*>.isFinal: Boolean get() = resolvedStatus.modality == Modality.FINAL

inline val FirCallableSymbol<*>.visibility: Visibility get() = resolvedStatus.visibility
inline val FirCallableSymbol<*>.effectiveVisibility: EffectiveVisibility get() = resolvedStatus.effectiveVisibility

inline val FirCallableSymbol<*>.isActual: Boolean get() = rawStatus.isActual
inline val FirCallableSymbol<*>.isExpect: Boolean get() = rawStatus.isExpect
inline val FirCallableSymbol<*>.isInner: Boolean get() = rawStatus.isInner
inline val FirCallableSymbol<*>.isStatic: Boolean get() = rawStatus.isStatic
inline val FirCallableSymbol<*>.isOverride: Boolean get() = resolvedStatus.isOverride
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

inline val FirClassLikeSymbol<*>.modality: Modality get() = resolvedStatus.modality
inline val FirClassLikeSymbol<*>.isAbstract: Boolean get() = resolvedStatus.modality == Modality.ABSTRACT
inline val FirClassLikeSymbol<*>.isFinal: Boolean get() = resolvedStatus.modality == Modality.FINAL

inline val FirClassLikeSymbol<*>.visibility: Visibility get() = rawStatus.visibility
inline val FirClassLikeSymbol<*>.effectiveVisibility: EffectiveVisibility get() = resolvedStatus.effectiveVisibility

inline val FirClassLikeSymbol<*>.isActual: Boolean get() = rawStatus.isActual
inline val FirClassLikeSymbol<*>.isExpect: Boolean get() = rawStatus.isExpect
inline val FirClassLikeSymbol<*>.isInner: Boolean get() = rawStatus.isInner
inline val FirClassLikeSymbol<*>.isStatic: Boolean get() = rawStatus.isStatic

@SuspiciousValueClassCheck
inline val FirClassLikeSymbol<*>.isInline: Boolean get() = rawStatus.isInline

@SuspiciousValueClassCheck
inline val FirClassLikeSymbol<*>.isValue: Boolean get() = rawStatus.isValue

@OptIn(SuspiciousValueClassCheck::class)
inline val FirClassLikeSymbol<*>.isInlineOrValue: Boolean get() = isInline || isValue

inline val FirClassLikeSymbol<*>.isExternal: Boolean get() = rawStatus.isExternal
inline val FirClassLikeSymbol<*>.isFromSealedClass: Boolean get() = rawStatus.isFromSealedClass
inline val FirClassLikeSymbol<*>.isFromEnumClass: Boolean get() = rawStatus.isFromEnumClass
inline val FirClassLikeSymbol<*>.isFun: Boolean get() = rawStatus.isFun
inline val FirClassLikeSymbol<*>.isCompanion: Boolean get() = rawStatus.isCompanion
inline val FirClassLikeSymbol<*>.isData: Boolean get() = rawStatus.isData
inline val FirClassLikeSymbol<*>.isSealed: Boolean get() = resolvedStatus.modality == Modality.SEALED

// ---------------------- common classes ----------------------

/**
 * As a rule of thumb, a class / typealias is considered local if its parent is another local class,
 * or if its parent is a callable declaration of any kind (function, property, constructor, enum entry, etc.).
 * A local class / typealias always has Local visibility, and vice versa.
 */
inline val FirClassLikeSymbol<*>.isLocal: Boolean get() = fir.isLocal

val FirBasedSymbol<*>?.isLocalClassLike: Boolean get() = (this as? FirClassLikeSymbol<*>)?.isLocal == true

inline val FirClassSymbol<*>.isClass: Boolean
    get() = classKind.isClass

inline val FirClassSymbol<*>.isInterface: Boolean
    get() = classKind.isInterface

inline val FirClassSymbol<*>.isEnumClass: Boolean
    get() = classKind.isEnumClass

inline val FirClassSymbol<*>.isEnumEntry: Boolean
    get() = classKind.isEnumEntry

// ---------------------- specific callables ----------------------
