/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirElementInterface

/**
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder.declarationStatus]
 */
interface FirDeclarationStatus : FirElementInterface {
    override val source: KtSourceElement?
    val visibility: Visibility
    val modality: Modality?
    val isExpect: Boolean
    val isActual: Boolean
    val isOverride: Boolean
    val isOperator: Boolean
    val isInfix: Boolean
    val isInline: Boolean
    val isTailRec: Boolean
    val isExternal: Boolean
    val isConst: Boolean
    val isLateInit: Boolean
    val isInner: Boolean
    val isCompanion: Boolean
    val isData: Boolean
    val isSuspend: Boolean
    val isStatic: Boolean
    val isFromSealedClass: Boolean
    val isFromEnumClass: Boolean
    val isFun: Boolean
    val hasStableParameterNames: Boolean
}
