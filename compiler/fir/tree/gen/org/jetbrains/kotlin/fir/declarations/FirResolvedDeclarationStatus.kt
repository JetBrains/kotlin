/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility

/**
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.resolvedDeclarationStatus]
 */
interface FirResolvedDeclarationStatus : FirDeclarationStatus {
    override val source: KtSourceElement?
    override val visibility: Visibility
    override val isExpect: Boolean
    override val isActual: Boolean
    override val isOverride: Boolean
    override val isOperator: Boolean
    override val isInfix: Boolean
    override val isInline: Boolean
    override val isTailRec: Boolean
    override val isExternal: Boolean
    override val isConst: Boolean
    override val isLateInit: Boolean
    override val isInner: Boolean
    override val isCompanion: Boolean
    override val isData: Boolean
    override val isSuspend: Boolean
    override val isStatic: Boolean
    override val isFromSealedClass: Boolean
    override val isFromEnumClass: Boolean
    override val isFun: Boolean
    override val hasStableParameterNames: Boolean
    override val modality: Modality
    val effectiveVisibility: EffectiveVisibility
}
