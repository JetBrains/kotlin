/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol

/**
 * Describes the reason why a certain classifier is considered as partially linked. See [LinkedClassifierExplorer.exploreSymbol].
 *
 * Can also be used as a result of exploring IR types to find out if the type uses any partially linked classifier.
 * See [LinkedClassifierExplorer.exploreType].
 */
internal sealed interface LinkedClassifierStatus {
    /** Indicates partially linked classifier. */
    sealed interface Partially : LinkedClassifierStatus {
        val symbol: IrClassifierSymbol

        sealed interface CanBeRootCause : Partially

        /**
         * There is no real owner classifier for the symbol, only synthetic stub created by [MissingDeclarationStubGenerator].
         * Likely the classifier has been deleted in newer version of the library.
         */
        @JvmInline
        value class MissingClassifier(override val symbol: IrClassifierSymbol) : CanBeRootCause

        /**
         * There is no enclosing class for inner class (or enum entry). This might happen if the inner class became a top-level class
         * in newer version of the library.
         */
        @JvmInline
        value class MissingEnclosingClass(override val symbol: IrClassSymbol) : CanBeRootCause

        /**
         * The classifier depends on another partially linked classifier. Thus, it is considered partially linked as well.
         */
        class DueToOtherClassifier(override val symbol: IrClassifierSymbol, val rootCause: CanBeRootCause) : Partially
    }

    /** Indicates fully linked classifier. */
    object Fully : LinkedClassifierStatus
}
