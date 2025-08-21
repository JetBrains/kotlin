/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

/**
 * This OptIn is used to mark direct access of .declarations property of FirFile/FirClass/FirScript, see KT-75498.
 *
 * Direct access to .declarations can be risky for various reasons:
 * - one doesn't see any plugin-generated declarations
 * - in IDE mode, there are no guarantees about a reached resolve phase
 *
 * It's recommended, especially in checkers, to use scope-based methods,
 * like processAllDeclarations, processAllDeclaredCallables, declaredProperties, declaredFunctions, constructors, etc.
 * The typical way looks like this:
 * ```
 * someClass(Symbol).processAllDeclarations(session) { it: FirBasedSymbol<*> ->
 *     // it is a declaration symbol
 * }
 * ```
 */
@RequiresOptIn(
    message = "Please use FirClass(Symbol).processAllDeclarations or similar functions instead, see also KDoc of DirectDeclarationsAccess"
)
annotation class DirectDeclarationsAccess
