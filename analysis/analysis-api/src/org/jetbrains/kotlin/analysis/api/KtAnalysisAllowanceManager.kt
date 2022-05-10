/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api

public object KtAnalysisAllowanceManager {
    @OptIn(KtAnalysisApiInternals::class)
    public inline fun <R> forbidAnalysisInside(actionName: String, action: () -> R): R {
        if (resolveIsForbiddenInActionWithName.get() != null) return action()
        resolveIsForbiddenInActionWithName.set(actionName)
        return try {
            action()
        } finally {
            resolveIsForbiddenInActionWithName.set(null)
        }
    }

    @KtAnalysisApiInternals
    public val resolveIsForbiddenInActionWithName: ThreadLocal<String?> = ThreadLocal.withInitial { null }
}