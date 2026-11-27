/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.components.checkValidity
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.assertIsValidAndAccessible

/**
 * A [KaLifetimeOwner] which belongs to a specific [KaFirSession] and implements public Analysis API endpoints on top of it.
 */
internal interface KaFirSessionBoundOwner : KaLifetimeOwner {
    val analysisSession: KaFirSession

    override val token: KaLifetimeToken
        get() = analysisSession.token
}

/**
 * Runs the implementation of a public Analysis API endpoint with the [analysisSession][KaFirSessionBoundOwner.analysisSession] as
 * a context parameter, after asserting that the session is valid and accessible.
 */
internal inline fun <R> KaFirSessionBoundOwner.entryPointImplementation(action: context(KaFirSession) () -> R): R {
    assertIsValidAndAccessible()
    return action(analysisSession)
}

/**
 * Runs the implementation of a public Analysis API endpoint with the [analysisSession][KaFirSessionBoundOwner.analysisSession] as
 * a context parameter, after asserting that the session is valid and accessible and that [element] can be analyzed in it.
 */
internal inline fun <R> KaFirSessionBoundOwner.entryPointImplementation(
    element: PsiElement?,
    action: context(KaFirSession) () -> R,
): R {
    assertIsValidAndAccessible()
    element?.checkValidity()
    return action(analysisSession)
}

/**
 * Runs the implementation of a public Analysis API endpoint with the [analysisSession][KaFirSessionBoundOwner.analysisSession] as
 * a context parameter, after asserting that the session is valid and accessible and that all [elements] can be analyzed in it.
 */
internal inline fun <R> KaFirSessionBoundOwner.entryPointImplementation(
    vararg elements: PsiElement?,
    action: context(KaFirSession) () -> R,
): R {
    assertIsValidAndAccessible()
    for (element in elements) {
        element?.checkValidity()
    }

    return action(analysisSession)
}

/**
 * Runs the implementation of a public Analysis API endpoint with the [analysisSession][KaFirSessionBoundOwner.analysisSession] as
 * a context parameter, after asserting that the session is valid and accessible and that all [elements] can be analyzed in it.
 */
internal inline fun <R> KaFirSessionBoundOwner.entryPointImplementation(
    elements: Iterable<PsiElement?>,
    action: context(KaFirSession) () -> R,
): R {
    assertIsValidAndAccessible()
    for (element in elements) {
        element?.checkValidity()
    }

    return action(analysisSession)
}
