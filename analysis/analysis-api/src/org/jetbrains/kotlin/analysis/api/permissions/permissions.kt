/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.permissions

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.permissions.KaAnalysisPermissionRegistry.KaExplicitAnalysisRestriction

/**
 * Forbids [analyze][org.jetbrains.kotlin.analysis.api.analyze] to be called in the given [action].
 *
 * @param description A human-readable description of the [action], which is used to generate error messages when
 *  [analyze][org.jetbrains.kotlin.analysis.api.analyze] is called.
 */
@OptIn(KaAnalysisApiInternals::class)
public inline fun <R> forbidAnalysis(description: String, action: () -> R): R {
    val permissionRegistry = KaAnalysisPermissionRegistry.getInstance()
    if (permissionRegistry.explicitAnalysisRestriction != null) return action()

    permissionRegistry.explicitAnalysisRestriction = KaExplicitAnalysisRestriction(description)
    return try {
        action()
    } finally {
        permissionRegistry.explicitAnalysisRestriction = null
    }
}

@RequiresOptIn("Analysis should not be allowed to be run from the EDT, as otherwise it may cause IDE freezes.")
public annotation class KaAllowAnalysisOnEdt

/**
 * Allows [analyze][org.jetbrains.kotlin.analysis.api.analyze] to be called on the EDT in the given [action], which is normally not allowed.
 *
 * Analysis is not supposed to be invoked from the EDT, as it may cause freezes. Use at your own risk!
 */
@KaAllowAnalysisOnEdt
@OptIn(KaAnalysisApiInternals::class)
public inline fun <T> allowAnalysisOnEdt(action: () -> T): T {
    val permissionRegistry = KaAnalysisPermissionRegistry.getInstance()
    if (permissionRegistry.isAnalysisAllowedOnEdt) return action()

    permissionRegistry.isAnalysisAllowedOnEdt = true
    try {
        return action()
    } finally {
        permissionRegistry.isAnalysisAllowedOnEdt = false
    }
}

@RequiresOptIn("Analysis should not be allowed to be run from a write action, as otherwise it may cause incorrect behavior and IDE freezes.")
public annotation class KaAllowAnalysisFromWriteAction

/**
 * The private [KaAllowProhibitedAnalyzeFromWriteAction] opt-in forces users of [allowAnalysisFromWriteAction] to specify an opt-in not only
 * in the code (via [KaAllowAnalysisFromWriteAction]), but also from the command line:
 *
 * ```
 * -opt-in=org.jetbrains.kotlin.analysis.api.permissions.KaAllowProhibitedAnalyzeFromWriteAction
 * ```
 *
 * This results in a double layer of opt-ins which makes it harder to abuse [allowAnalysisFromWriteAction].
 */
@RequiresOptIn("Analysis should be prohibited to be ran from write action, otherwise it may cause IDE freezes and incorrect behavior in some cases")
private annotation class KaAllowProhibitedAnalyzeFromWriteAction

/**
 * Allows [analyze][org.jetbrains.kotlin.analysis.api.analyze] to be called from a write action in the given [action], which is normally
 * not allowed.
 *
 * Analysis is not supposed to be called from a write action.
 * Such actions can lead to IDE freezes and incorrect behavior in some cases.
 *
 * There is no guarantee that PSI changes will be reflected in an Analysis API world inside
 * one [analyze][org.jetbrains.kotlin.analysis.api.analyze] session.
 * Example:
 * ```
 * // code to be analyzed
 * fun foo(): Int = 0
 *
 * // use case code
 * fun useCase() {
 *   analyse(function) {
 *    // 'getConstantFromExpressionBody' is an imaginary function
 *    val valueBefore = function.getConstantFromExpressionBody() // valueBefore is 0
 *
 *    changeExpressionBodyTo(1) // now function will looks like `fun foo(): Int = 1`
 *    val valueAfter = function.getConstantFromExpressionBody() // Wrong way: valueAfter is not guarantied to be '1'
 *   }
 *
 *   analyse(function) {
 *    val valueAfter = function.getConstantFromExpressionBody() // OK: valueAfter is guarantied to be '1'
 *   }
 * }
 * ```
 */
@KaAllowAnalysisFromWriteAction
@KaAllowProhibitedAnalyzeFromWriteAction
@OptIn(KaAnalysisApiInternals::class)
public inline fun <T> allowAnalysisFromWriteAction(action: () -> T): T {
    val permissionRegistry = KaAnalysisPermissionRegistry.getInstance()
    if (permissionRegistry.isAnalysisAllowedInWriteAction) return action()

    permissionRegistry.isAnalysisAllowedInWriteAction = true
    try {
        return action()
    } finally {
        permissionRegistry.isAnalysisAllowedInWriteAction = false
    }
}
