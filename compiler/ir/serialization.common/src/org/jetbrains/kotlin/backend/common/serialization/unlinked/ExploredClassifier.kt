/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol

/**
 * Describes the reason why a certain classifier is considered as unusable (partially linked).
 * For more details see [ClassifierExplorer.exploreSymbol].
 */
internal sealed interface ExploredClassifier {
    /** Indicated unusable classifier. */
    sealed interface Unusable : ExploredClassifier {
        val symbol: IrClassifierSymbol

        sealed interface CanBeRootCause : Unusable

        /**
         * There is no real owner classifier for the symbol, only synthetic stub created by [MissingDeclarationStubGenerator].
         * Likely the classifier has been deleted in newer version of the library.
         */
        data class MissingClassifier(override val symbol: IrClassifierSymbol) : CanBeRootCause

        /**
         * There is no enclosing class for inner class (or enum entry). This might happen if the inner class became a top-level class
         * in newer version of the library.
         */
        data class MissingEnclosingClass(override val symbol: IrClassSymbol) : CanBeRootCause


        /**
         * The classifier depends on another unusable classifier. Thus, it is considered unusable too.
         */
        data class DueToOtherClassifier(override val symbol: IrClassifierSymbol, val rootCause: CanBeRootCause) : Unusable
    }

    /** Indicates usable (fully linked) classifier. */
    object Usable : ExploredClassifier
}
