/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.expressions

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.analysis.api.resolution.KaContextSensitiveResolutionStatus
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

/**
 * Checks if the [KtSimpleNameExpression] is an implicit reference to a companion object via the containing class.
 *
 * #### Example
 *
 * ```
 * class A {
 *    companion object {
 *       fun foo() {}
 *    }
 * }
 * ```
 *
 * Given a call `A.foo()`, `A` is an implicit reference to the companion object, so `isImplicitReferenceToCompanion` returns `true`.
 */
context(session: KaSession)
public val KtSimpleNameExpression.isImplicitReferenceToCompanion: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.resolver.isImplicitReferenceToCompanion(this)
    }

/**
 * The [context-sensitive resolution](https://github.com/Kotlin/KEEP/issues/379) status of the [KtSimpleNameExpression]:
 * whether the name is already resolved through context-sensitive resolution, and whether a redundant explicit
 * qualifier or import could be removed in favor of it.
 *
 * The information is available even when the `-Xcontext-sensitive-resolution` feature is not enabled.
 *
 * #### Example
 *
 * ```
 * enum class Foo { BAR }
 *
 * fun usage(): Foo {
 *     return Foo.BAR // the 'Foo.' qualifier can be removed -> KaContextSensitiveResolutionStatus.QualifierCanBeRemoved
 * }
 * ```
 *
 * @see KaContextSensitiveResolutionStatus
 */
@KaExperimentalApi
context(session: KaSession)
public val KtSimpleNameExpression.contextSensitiveResolutionStatus: KaContextSensitiveResolutionStatus
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.resolver.contextSensitiveResolutionStatus(this)
    }
