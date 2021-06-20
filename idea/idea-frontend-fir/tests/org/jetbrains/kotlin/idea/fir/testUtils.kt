/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import org.jetbrains.kotlin.idea.fir.low.level.api.element.builder.DuplicatedFirSourceElementsException
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File


inline fun <T> runReadAction(crossinline runnable: () -> T): T {
    return ApplicationManager.getApplication().runReadAction(Computable { runnable() })
}

fun <R> executeOnPooledThreadInReadAction(action: () -> R): R =
    ApplicationManager.getApplication().executeOnPooledThread<R> { runReadAction(action) }.get()

inline fun <R> analyseOnPooledThreadInReadAction(context: KtElement, crossinline action: KtAnalysisSession.() -> R): R =
    executeOnPooledThreadInReadAction {
        analyse(context) { action() }
    }


/**
 * Temporary
 * @see org.jetbrains.kotlin.idea.fir.low.level.api.DuplicatedFirSourceElementsException.IS_ENABLED
 */
inline fun <T> withPossiblyDisabledDuplicatedFirSourceElementsException(fileText: String, action: () -> T): T {
    val isDisabled = InTextDirectivesUtils.isDirectiveDefined(fileText, "IGNORE_DUPLICATED_FIR_SOURCE_EXCEPTION")

    @Suppress("LiftReturnOrAssignment")
    if (isDisabled) {
        val wasEnabled = DuplicatedFirSourceElementsException.IS_ENABLED
        DuplicatedFirSourceElementsException.IS_ENABLED = false
        try {
            return action()
        } finally {
            DuplicatedFirSourceElementsException.IS_ENABLED = wasEnabled
        }
    } else {
        return action()
    }
}
