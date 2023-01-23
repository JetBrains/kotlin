/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
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
         * There is an issue with inheritance: interface inherits from a class, class inherits from a final class, etc.
         * On practice, such class can't be instantiated and used anywhere.
         */
        class InvalidInheritance private constructor(
            override val symbol: IrClassSymbol,
            val superClassSymbols: Collection<IrClassSymbol>,
            val unexpectedSuperClassConstructorSymbol: IrConstructorSymbol?
        ) : CanBeRootCause {
            constructor(symbol: IrClassSymbol, superClassSymbols: Collection<IrClassSymbol>) : this(symbol, superClassSymbols, null)

            constructor(
                symbol: IrClassSymbol,
                superClassSymbol: IrClassSymbol,
                unexpectedSuperClassConstructorSymbol: IrConstructorSymbol
            ) : this(symbol, listOf(superClassSymbol), unexpectedSuperClassConstructorSymbol)

            init {
                // Just a sanity check to avoid creating invalid [InvalidInheritance]s.
                check(superClassSymbols.isNotEmpty())
            }
        }

        /**
         * The annotation class has unacceptable classifier as one of its parameters: not one of permitted classes ([String], [KClass]),
         * primitives, etc. This may happen if the class representing this parameter was an annotation class before, but later it was
         * converted to a non-annotation class.
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
