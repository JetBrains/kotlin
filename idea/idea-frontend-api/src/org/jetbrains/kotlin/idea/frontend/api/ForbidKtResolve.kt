/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

@RequiresOptIn
public annotation class ForbidKtResolveInternals

public object ForbidKtResolve {
    @OptIn(ForbidKtResolveInternals::class)
    public inline fun <R> forbidResolveIn(actionName: String, action: () -> R): R {
        if (resovleIsForbidenInActionWithName.get() != null) return action()
        resovleIsForbidenInActionWithName.set(actionName)
        return try {
            action()
        } finally {
            resovleIsForbidenInActionWithName.set(null)
        }
    }

    @ForbidKtResolveInternals
    public val resovleIsForbidenInActionWithName: ThreadLocal<String?> = ThreadLocal.withInitial { null }
}