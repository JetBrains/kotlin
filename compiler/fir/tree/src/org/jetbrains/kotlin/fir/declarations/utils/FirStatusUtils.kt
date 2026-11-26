/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.utils

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

inline val FirMemberDeclaration.modality: Modality? get() = status.modality
inline val FirMemberDeclaration.isAbstract: Boolean get() = status.modality == Modality.ABSTRACT
inline val FirMemberDeclaration.isOpen: Boolean get() = status.modality == Modality.OPEN
inline val FirMemberDeclaration.isFinal: Boolean
    get() {
        // member with unspecified modality is final
        val modality = status.modality ?: return true
        return modality == Modality.FINAL
    }

inline val FirMemberDeclaration.visibility: Visibility get() = status.visibility

/**
 * Gets the effective visibility. Note that it's assumed that the element or its non-local container has at least resolve phase
 * [FirResolvePhase.STATUS], in which case, any declarations with unresolved status are effectively local.
 */
inline val FirMemberDeclaration.effectiveVisibility: EffectiveVisibility
    get() = (status as? FirResolvedDeclarationStatus)?.effectiveVisibility ?: EffectiveVisibility.Local

inline val FirMemberDeclaration.isOverridable: Boolean
    get() = status.modality != Modality.FINAL && status.visibility != Visibilities.Private

inline val FirMemberDeclaration.isActual: Boolean get() = status.isActual
inline val FirMemberDeclaration.isExpect: Boolean get() = status.isExpect


inline val FirClassLikeDeclaration.isInner: Boolean get() = status.isInner
inline val FirCallableDeclaration.isStatic: Boolean get() = status.isStatic
inline val FirCallableDeclaration.isOverride: Boolean get() = status.isOverride
inline val FirCallableDeclaration.isOperator: Boolean get() = status.isOperator
inline val FirCallableDeclaration.isInfix: Boolean get() = status.isInfix
inline val FirCallableDeclaration.isInline: Boolean get() = status.isInline
inline val FirClassLikeDeclaration.isInline: Boolean get() = status.isInline

inline val FirClass.isValue: Boolean get() = status.isValue

@Deprecated("Use `isValue` instead.", ReplaceWith("isValue"), level = ERROR)
inline val FirClass.isInlineOrValue: Boolean get() = status.isValue

inline val FirCallableDeclaration.isTailRec: Boolean get() = status.isTailRec
inline val FirMemberDeclaration.isExternal: Boolean get() = status.isExternal
inline val FirCallableDeclaration.isSuspend: Boolean get() = status.isSuspend
inline val FirCallableDeclaration.isConst: Boolean get() = status.isConst
inline val FirCallableDeclaration.isLateInit: Boolean get() = status.isLateInit
inline val FirMemberDeclaration.isFromSealedClass: Boolean get() = status.isFromSealedClass
inline val FirMemberDeclaration.isFromEnumClass: Boolean get() = status.isFromEnumClass
inline val FirClassLikeDeclaration.isFun: Boolean get() = status.isFun
inline val FirCallableDeclaration.hasStableParameterNames: Boolean get() = status.hasStableParameterNames

inline val FirClass.isInterface: Boolean
    get() = classKind.isInterface

inline val FirClass.isEnumClass: Boolean
    get() = classKind.isEnumClass

inline val FirRegularClass.isSealed: Boolean get() = status.modality == Modality.SEALED

inline val FirRegularClass.canHaveAbstractDeclaration: Boolean
    get() = isInterface || isAbstract || isSealed || isEnumClass

inline val FirRegularClassSymbol.canHaveAbstractDeclaration: Boolean
    get() = fir.canHaveAbstractDeclaration

inline val FirRegularClass.isCompanion: Boolean get() = status.isCompanion
inline val FirRegularClass.isData: Boolean get() = status.isData
inline val FirRegularClass.isRichError: Boolean get() = status.isRichError

inline val FirFunction.hasBody: Boolean get() = body != null

inline val FirPropertyAccessor.hasBody: Boolean get() = body != null

val FirCallableDeclaration.isCompanionBlockMember: Boolean
    get() = isStatic && containingClassForStaticMemberAttr != null

val FirCallableDeclaration.isCompanionExtension: Boolean
    get() = isStatic && containingClassForStaticMemberAttr == null && receiverParameter != null
