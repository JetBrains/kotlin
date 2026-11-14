/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef

/**
 * Allows compiler plugins to resolve types in yet to be resolved code.
 * For example, this can be used to resolve annotation arguments or type parameter bounds
 * to aid in generating declarations with the appropriate types.
 * Types are resolved within a particular scope based on the extension being used.
 *
 * # Declaration Generation
 *
 * For [FirDeclarationGenerationExtension], this is relative to the **outer scope** of the
 * [context owner][DeclarationGenerationContext.owner].
 *
 * ```kotlin
 * import org.example.*
 *
 * @Ann(Bar::class)
 * class Foo<T> {
 *     interface Nested
 *     inner class Inner
 * }
 * ```
 *
 * - When generating declarations within `Foo`, the scope of resolution will be the file
 * and include imports and all top-level classifiers.
 * Attempts to resolve `Foo.Nested` or `T` will result in an [error type][org.jetbrains.kotlin.fir.types.FirErrorTypeRef].
 *
 * - When generating declarations within `Foo.Nested`, the scope of resolution will be the static-level `Foo` class
 * and include imports, all top-level classifiers, and all nested classifiers within `Foo`.
 * Attempts to resolve `T` will result in an [error type][org.jetbrains.kotlin.fir.types.FirErrorTypeRef].
 *
 * - When generating declarations within `Foo.Inner`, the scope of resolution will be the instance-level `Foo` class
 * and include imports, all top-level classifiers, all nested classifiers within `Foo`, and type parameters of `Foo`.
 *
 * This means that depending on what type is being resolved, and from what scope,
 * it is possible to resolve to an error type even if the used type is valid.
 *
 * # Supertype Generation
 *
 * For [FirSupertypeGenerationExtension], this is relative to the **outer scope** of the specified class.
 * This means all the same examples as above for declaration generation hold when adding additional supertypes.
 *
 * @see typeFromQualifierParts
 */
interface PluginTypeResolver {
    /**
     * Resolves a [user type][FirUserTypeRef].
     * The resolved type may be an [error][org.jetbrains.kotlin.fir.types.FirErrorTypeRef].
     */
    fun resolveUserType(type: FirUserTypeRef): FirResolvedTypeRef
}
