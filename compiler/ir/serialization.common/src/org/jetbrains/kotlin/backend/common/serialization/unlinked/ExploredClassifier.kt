/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import kotlin.reflect.KClass

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
         * An annotation class uses unacceptable parameter in its own constructor: not one of permitted classes ([String], [KClass]),
         * primitives, etc.
         */
        data class AnnotationWithUnacceptableParameter(
            override val symbol: IrClassSymbol,
            val unacceptableClassifierSymbol: IrClassifierSymbol
        ) : CanBeRootCause

        /**
         * The classifier depends on another unusable classifier. Thus, it is considered unusable too.
         */
        data class DueToOtherClassifier(override val symbol: IrClassifierSymbol, val rootCause: CanBeRootCause) : Unusable
    }

    /** Indicates usable (fully linked) classifier. */
    object Usable : ExploredClassifier
}
