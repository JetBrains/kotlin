/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.render

inline val FirStatusOwner.modality: Modality? get() = status.modality
inline val FirStatusOwner.isAbstract: Boolean get() = status.modality == Modality.ABSTRACT
inline val FirStatusOwner.isOpen: Boolean get() = status.modality == Modality.OPEN
inline val FirStatusOwner.isFinal: Boolean
    get() {
        // member with unspecified modality is final
        val modality = status.modality ?: return true
        return modality == Modality.FINAL
    }

inline val FirStatusOwner.visibility: Visibility get() = status.visibility
inline val FirStatusOwner.effectiveVisibility: EffectiveVisibility
    get() = (status as? FirResolvedDeclarationStatus)?.effectiveVisibility
        ?: error("Effective visibility for ${render(FirRenderer.RenderMode.NoBodies)} must be resolved")

inline val FirStatusOwner.allowsToHaveFakeOverride: Boolean
    get() = !Visibilities.isPrivate(visibility) && visibility != Visibilities.InvisibleFake

inline val FirStatusOwner.isActual: Boolean get() = status.isActual
inline val FirStatusOwner.isExpect: Boolean get() = status.isExpect
inline val FirStatusOwner.isInner: Boolean get() = status.isInner
inline val FirStatusOwner.isStatic: Boolean get() = status.isStatic
inline val FirStatusOwner.isOverride: Boolean get() = status.isOverride
inline val FirStatusOwner.isOperator: Boolean get() = status.isOperator
inline val FirStatusOwner.isInfix: Boolean get() = status.isInfix
inline val FirStatusOwner.isInline: Boolean get() = status.isInline
inline val FirStatusOwner.isTailRec: Boolean get() = status.isTailRec
inline val FirStatusOwner.isExternal: Boolean get() = status.isExternal
inline val FirStatusOwner.isSuspend: Boolean get() = status.isSuspend
inline val FirStatusOwner.isConst: Boolean get() = status.isConst
inline val FirStatusOwner.isLateInit: Boolean get() = status.isLateInit
inline val FirStatusOwner.isFromSealedClass: Boolean get() = status.isFromSealedClass
inline val FirStatusOwner.isFromEnumClass: Boolean get() = status.isFromEnumClass
inline val FirStatusOwner.isFun: Boolean get() = status.isFun

inline val FirClassLikeDeclaration<*>.isLocal: Boolean get() = symbol.classId.isLocal

inline val FirClass<*>.isInterface: Boolean
    get() = classKind == ClassKind.INTERFACE

inline val FirClass<*>.isEnumClass: Boolean
    get() = classKind == ClassKind.ENUM_CLASS

inline val FirRegularClass.isSealed: Boolean get() = status.modality == Modality.SEALED

inline val FirRegularClass.canHaveAbstractDeclaration: Boolean
    get() = isAbstract || isSealed || isEnumClass

inline val FirRegularClass.isCompanion: Boolean get() = status.isCompanion
inline val FirRegularClass.isData: Boolean get() = status.isData

inline val FirFunction<*>.hasBody: Boolean get() = body != null

inline val FirPropertyAccessor.hasBody: Boolean get() = body != null
inline val FirPropertyAccessor.allowsToHaveFakeOverride: Boolean get() = visibility.allowsToHaveFakeOverride

inline val FirProperty.allowsToHaveFakeOverride: Boolean get() = visibility.allowsToHaveFakeOverride

inline val Visibility.allowsToHaveFakeOverride: Boolean
    get() = !Visibilities.isPrivate(this) && this != Visibilities.InvisibleFake

inline val FirSimpleFunction.isLocal: Boolean get() = status.visibility == Visibilities.Local
