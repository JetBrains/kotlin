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
 * - in IDE mode, there is no guarantees about resolve phase
 *
 * It's recommended to use scope-based methods, like processAllDeclarations, processAllDeclaredCallables etc.
 */
@RequiresOptIn(message = "Please use processAllDeclarations or similar functions instead")
annotation class DirectDeclarationsAccess
