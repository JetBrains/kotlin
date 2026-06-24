/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail

/**
 * Describes the implementation state of a [KaCallableSymbol] in the context of a specific [KaClassSymbol].
 *
 * An implementation state captures whether a callable is explicitly implemented in the class, has an inherited
 * implementation, can be overridden, or must be explicitly overridden.
 *
 * @see implementationState
 */
@KaExperimentalApi
public sealed interface KaCallableImplementationState {
    /**
     * The declaration is directly implemented or explicitly overridden in the target class.
     */
    @KaExperimentalApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface Explicit : KaCallableImplementationState {
        /**
         * Whether the implementation is complete. E.g., for a `var` property implemented by `val`, [isComplete] will be `false`.
         */
        public val isComplete: Boolean
    }

    /**
     * The declaration has the implementation provided by a supertype or multiple supertypes, and **does not** have explicit implementation
     * in the target class.
     */
    @KaExperimentalApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface Inherited : KaCallableImplementationState {
        /**
         * Whether multiple supertypes provide implementations.
         * As the compiler cannot decide which implementation to choose, the declaration must be overridden explicitly. E.g.:
         *
         * ```kotlin
         * interface ColoredEntity {
         *     val color: String
         * }
         *
         * interface GreenEntity : ColoredEntity {
         *     override val color get() = "green"
         * }
         *
         * interface BlueEntity : ColoredEntity {
         *     override val color get() = "blue"
         * }
         *
         * // Interface 'SeaColorEntity' must override 'color' because it inherits multiple interface methods for it
         * interface SeaColorEntity : GreenEntity, BlueEntity
         * ```
         */
        public val isAmbiguous: Boolean

        /**
         * Whether the declaration can be overridden in the target class (e.g., it is not marked as `final` in a supertype).
         */
        public val isOverridable: Boolean
    }

    /**
     * The declaration is neither implemented in the target class, nor it has inherited implementations.
     *
     * Note that it does not necessarily mean it is a compilation error – if the target class is `abstract`, the implementation
     * can legitimately be absent.
     */
    @KaExperimentalApi
    @SubclassOptInRequired(KaImplementationDetail::class)
    public interface Missing : KaCallableImplementationState

    @Suppress("unused")
    @KaExperimentalApi
    private object Unknown : KaCallableImplementationState {
        override fun toString(): String = "Unknown"
    }
}
