/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.util.*
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.diagnostics.FirDiagnosticHolder
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isObjectLiteral
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock


internal inline fun <T> executeWithoutPCE(crossinline action: () -> T): T {
    var result: T? = null
    ProgressManager.getInstance().executeNonCancelableSection { result = action() }
    @Suppress("UNCHECKED_CAST")
    return result as T
}

internal inline fun <T> Lock.lockWithPCECheck(lockingIntervalMs: Long, action: () -> T): T {
    var needToRun = true
    var result: T? = null
    while (needToRun) {
        checkCanceled()
        if (tryLock(lockingIntervalMs, TimeUnit.MILLISECONDS)) {
            try {
                needToRun = false
                result = action()
            } finally {
                unlock()
            }
        }
    }
    return result!!
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun checkCanceled() {
    ProgressManager.checkCanceled()
}

internal val FirElement.isErrorElement
    get() = this is FirDiagnosticHolder

internal val FirDeclaration.ktDeclaration: KtDeclaration
    get() {
        val psi = psi
            ?: errorWithFirSpecificEntries("PSI element was not found", fir = this)
        return when (psi) {
            is KtDeclaration -> psi
            is KtObjectLiteralExpression -> psi.objectDeclaration
            else -> errorWithFirSpecificEntries(
                "FirDeclaration.psi (${this::class.simpleName}) should be KtDeclaration but was ${psi::class.simpleName}",
                fir = this,
                psi = psi,
            )
        }
    }

internal val FirDeclaration.containingKtFileIfAny: KtFile?
    get() = psi?.containingFile as? KtFile



internal fun KtDeclaration.isNonAnonymousClassOrObject() =
    this is KtClassOrObject
            && !this.isObjectLiteral()

