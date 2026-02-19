/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.OnlyForDefaultLanguageFeatureDisabled

/**
 * Enum describing the different kinds of [FirInaccessibleReceiverExpression].
 *
 * @param producesApplicableCandidate true means that a resolution candidate with this kind of receiver will stop the tower resolution
 * and an error will be reported in a checker.
 */
enum class InaccessibleReceiverKind(
    val producesApplicableCandidate: Boolean
) {
    /**
     * Inaccessible `this` inside secondary constructor parameter default values and delegated constructor.
     *
     * ```kt
     * class Foo(...) {
     *   constructor(x: String = this.y) // INSTANCE_ACCESS_BEFORE_SUPER_CALL
     *     : this(this.y) // INSTANCE_ACCESS_BEFORE_SUPER_CALL
     * }
     * ```
     *
     * Only used when [LanguageFeature.ImprovedResolutionInSecondaryConstructors] is disabled because it stops the tower resolution and
     * prevents resolution to better applicable candidates.
     */
    @OnlyForDefaultLanguageFeatureDisabled(LanguageFeature.ImprovedResolutionInSecondaryConstructors)
    SecondaryConstructor(true),

    /**
     * Inaccessible outer class receiver of non-inner nested class
     *
     * ```kt
     * class Foo {
     *   class Bar {
     *     fun test() {
     *       this@Foo.x // INACCESSIBLE_OUTER_CLASS_RECEIVER
     *     }
     *   }
     * }
     * ```
     */
    OuterClassOfNonInner(false),

    /**
     * Inaccessible `this` inside class header. Is also used for secondary constructor parameter default values and delegated constructor
     * when [LanguageFeature.ImprovedResolutionInSecondaryConstructors] is enabled.
     *
     * ```kt
     * class Foo(val x: String = this.y) // INSTANCE_ACCESS_BEFORE_SUPER_CALL
     *  : Bar(this.y), // INSTANCE_ACCESS_BEFORE_SUPER_CALL
     *    Baz by this.y // INSTANCE_ACCESS_BEFORE_SUPER_CALL
     * ```
     */
    ClassHeader(false),
}