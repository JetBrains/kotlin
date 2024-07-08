/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

@RequiresOptIn("This implementation detail alters the checkers behavior in a subtle way. Are you sure this is what you want?")
annotation class CheckersCornerCase

/**
 * - [CheckerSessionKind.DeclarationSiteForExpectsPlatformForOthers] means that this checker should run from the same
 *   session to which corresponding declaration belongs
 * - [CheckerSessionKind.Platform] means that in case of MPP compilation this
 *   checker should run with session of leaf platform module for sources
 *   of all modules
 *
 *  For more information see the doc: compiler/fir/checkers/module.md
 */
enum class CheckerSessionKind {
    Platform,

    /**
     * If the checker visits a declaration that is an/inside an `expect` declaration,
     * it receives the declaration-site session of that declaration in its
     * [CheckerContext][org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext].
     * Otherwise, it's supplied with the leaf platform session.
     */
    DeclarationSiteForExpectsPlatformForOthers,

    /**
     * Makes the checker always receive the declaration-site session of the declaration
     * it visits.
     *
     * Please only use this kind for the following reasons and discuss the subject with
     * the Compiler Frontend team if nothing feels applicable:
     *   - The checker compares `moduleData` of different declarations
     *   - The checker checks the consistency of declarations in scopes of different
     *     declarations within the same module
     */
    @CheckersCornerCase
    DeclarationSite,
}

interface FirCheckerWithMppKind {
    val mppKind: CheckerSessionKind
}
