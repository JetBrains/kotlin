/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.modification

/**
 * [KaSourceModificationLocality] describes the scope of effect of a source modification detected by [KaSourceModificationService].
 */
public sealed interface KaSourceModificationLocality {
    /**
     * A change that has no effect on cached information.
     */
    public interface Invisible : KaSourceModificationLocality

    /**
     * Whitespace modification covers changes in whitespace and comments.
     *
     * It *usually* has no effect, but it can affect compiler diagnostics. For example, when we have `if (x) "a"else "b"`, the compiler
     * produces the error "literals must be surrounded by whitespace" (see KT-82629). Changing it to `if (x) "a" else "b"` fixes the
     * problem, but for the cached error to disappear, caches that can be affected by PSI-only changes need to be invalidated.
     *
     * Whitespace modification is distinct from [in-block modification][InBlock]. While in-block modification can affect both the syntax and
     * the semantics of the code, whitespace modification is guaranteed to only have a syntactic effect and preserve semantics. (Whitespace
     * deletions which might affect code semantics would be reported as [InBlock] or [OutOfBlock].) Since just the PSI is affected, only the
     * PSI-based subset of the compiler's checkers can be affected by such a change.
     *
     * Whitespace modification can occur in any location. Even if it occurs outside a declaration, whitespace modification only affects its
     * containing declaration or the file itself, not the whole module.
     */
    public interface Whitespace : KaSourceModificationLocality

    /**
     * In-block modification is a source code modification that doesn't affect the state of other non-local declarations.
     *
     * #### Example 1
     *
     * ```
     * val x: Int = 10<caret>
     * val z = x
     * ```
     *
     * If we change `10` to `"str"`, it would not change the type of `z`, so it is an **in-block-modification**.
     *
     * #### Example 2
     *
     * ```
     * val x = 10<caret>
     * val z = x
     * ```
     *
     * If we change the initializer of `x` to `"str"`, as in the first example,
     * the return type of `x` will become `String` instead of the initial `Int`.
     * This will change the return type of `z` as it does not have an explicit type.
     * So, it is an **out-of-block modification**.
     */
    public interface InBlock : KaSourceModificationLocality

    /**
     * Out-of-block modification is a source code modification that may affect the state of other declarations in the same module and the
     * declarations of dependent modules.
     *
     * #### Example 1
     *
     * ```
     * val x = 10<caret>
     * val z = x
     * ```
     *
     * If we change the initializer of `x` to `"str"` the return type of `x` will become `String` instead of the initial `Int`. This will
     * change the return type of `z` as it does not have an explicit type. So, it is an **out-of-block modification**.
     *
     * #### Example 2
     *
     * ```
     * val x: Int = 10<caret>
     * val z = x
     * ```
     *
     * If we change `10` to `"str"` as in the first example, it would not change the type of `z`, so it is not an **out-of-block-modification**.
     *
     * #### Examples of out-of-block modifications
     *
     *  - Modifying the body of a non-local declaration which doesn't have an explicit return type specified
     *  - Changing the package of a file
     *  - Adding a new declaration
     *  - Moving a declaration to another package
     *
     * Generally, all modifications that happen outside the body of a callable declaration (functions, accessors, or properties) with an
     * explicit type are considered **out-of-block**.
     */
    public interface OutOfBlock : KaSourceModificationLocality
}
