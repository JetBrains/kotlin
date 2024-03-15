/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/fir/tree/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.KtSourceElement

/**
 * Base interface for all fir elements. This interface is needed in order to add off-tree element types to the fir hierarchy.
 * The off-tree element type is a fir interface that is inherited from [FirElementInterface] like [org.jetbrains.kotlin.fir.expressions.FirStatement].
 * We should get rid of such interfaces and inherit all fir elements from [FirElement] in the future.
 *
 * **Notice:** every class implementing [FirElementInterface] must inherit [FirElement].
 *
 * Generated from: [org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder.Companion.baseFirElement]
 */
interface FirElementInterface {
    val source: KtSourceElement?
}
