/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.api

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

abstract class ResolveAllowanceService {
    abstract fun switchOnAllowingResolveInEdtInCurrentThread(): SwitchResult
    abstract fun switchOffAllowingResolveInEdtInCurrentThread()
    abstract fun isResolveOnEdtInCurrentThreadAllowed(): Boolean

    abstract fun switchOnForbidResolveInCurrentThread(actionName: String): SwitchResult
    abstract fun switchOffForbidResolveInCurrentThread()
    abstract fun getResolveInCurrentThreadForbiddenReason(): String?

    enum class SwitchResult {
        SWITCHED,
        ALREADY_SWITCHED
    }
}

inline fun <T> allowResolveInEdtIn(project: Project, block: () -> T): T {
    val resolveAllowanceService = project.service<ResolveAllowanceService>()
    return when (resolveAllowanceService.switchOnAllowingResolveInEdtInCurrentThread()) {
        ResolveAllowanceService.SwitchResult.SWITCHED -> {
            try {
                block()
            } finally {
                resolveAllowanceService.switchOffAllowingResolveInEdtInCurrentThread()
            }
        }
        ResolveAllowanceService.SwitchResult.ALREADY_SWITCHED -> {
            block()
        }
    }
}

inline fun <T> forbidResolveIn(project: Project, actionName: String, block: () -> T): T {
    val resolveAllowanceService = project.service<ResolveAllowanceService>()
    return when (resolveAllowanceService.switchOnForbidResolveInCurrentThread(actionName)) {
        ResolveAllowanceService.SwitchResult.SWITCHED -> {
            try {
                block()
            } finally {
                resolveAllowanceService.switchOffForbidResolveInCurrentThread()
            }
        }
        ResolveAllowanceService.SwitchResult.ALREADY_SWITCHED -> {
            block()
        }
    }
}
