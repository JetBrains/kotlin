/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations.utils

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.declarations.*

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

inline val FirMemberDeclaration.allowsToHaveFakeOverride: Boolean
    get() = !Visibilities.isPrivate(visibility) && visibility != Visibilities.InvisibleFake

inline val FirMemberDeclaration.isActual: Boolean get() = status.isActual
inline val FirMemberDeclaration.isExpect: Boolean get() = status.isExpect
inline val FirMemberDeclaration.isInner: Boolean get() = status.isInner
inline val FirMemberDeclaration.isStatic: Boolean get() = status.isStatic
inline val FirMemberDeclaration.isOverride: Boolean get() = status.isOverride
inline val FirMemberDeclaration.isOperator: Boolean get() = status.isOperator
inline val FirMemberDeclaration.isInfix: Boolean get() = status.isInfix
inline val FirMemberDeclaration.isInline: Boolean get() = status.isInline
inline val FirMemberDeclaration.isTailRec: Boolean get() = status.isTailRec
inline val FirMemberDeclaration.isExternal: Boolean get() = status.isExternal
inline val FirMemberDeclaration.isSuspend: Boolean get() = status.isSuspend
inline val FirMemberDeclaration.isConst: Boolean get() = status.isConst
inline val FirMemberDeclaration.isLateInit: Boolean get() = status.isLateInit
inline val FirMemberDeclaration.isFromSealedClass: Boolean get() = status.isFromSealedClass
inline val FirMemberDeclaration.isFromEnumClass: Boolean get() = status.isFromEnumClass
inline val FirMemberDeclaration.isFun: Boolean get() = status.isFun
inline val FirMemberDeclaration.hasStableParameterNames: Boolean get() = status.hasStableParameterNames

inline val FirClassLikeDeclaration.isLocal: Boolean get() = symbol.classId.isLocal

inline val FirClass.isInterface: Boolean
    get() = classKind.isInterface

inline val FirClass.isEnumClass: Boolean
    get() = classKind.isEnumClass

inline val FirRegularClass.isSealed: Boolean get() = status.modality == Modality.SEALED

inline val FirRegularClass.canHaveAbstractDeclaration: Boolean
    get() = isAbstract || isSealed || isEnumClass

inline val FirRegularClass.isCompanion: Boolean get() = status.isCompanion
inline val FirRegularClass.isData: Boolean get() = status.isData

inline val FirFunction.hasBody: Boolean get() = body != null

inline val FirPropertyAccessor.hasBody: Boolean get() = body != null
inline val FirPropertyAccessor.allowsToHaveFakeOverride: Boolean get() = visibility.allowsToHaveFakeOverride

inline val FirProperty.allowsToHaveFakeOverride: Boolean get() = visibility.allowsToHaveFakeOverride

inline val Visibility.allowsToHaveFakeOverride: Boolean
    get() = !Visibilities.isPrivate(this) && this != Visibilities.InvisibleFake

inline val FirSimpleFunction.isLocal: Boolean get() = status.visibility == Visibilities.Local
