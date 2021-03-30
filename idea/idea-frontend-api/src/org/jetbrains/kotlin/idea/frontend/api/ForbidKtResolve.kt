/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import kotlin.reflect.KProperty1

@RequiresOptIn
annotation class ForbidKtResolveInternals

object ForbidKtResolve {
    @OptIn(ForbidKtResolveInternals::class)
    inline fun <R> forbidResolveIn(actionName: String, action: () -> R): R {
        if (resovleIsForbidenInActionWithName.get() != null) return action()
        resovleIsForbidenInActionWithName.set(actionName)
        return try {
            action()
        } finally {
            resovleIsForbidenInActionWithName.set(null)
        }
    }

    @ForbidKtResolveInternals
    val resovleIsForbidenInActionWithName: ThreadLocal<String?> = ThreadLocal.withInitial { null }
}