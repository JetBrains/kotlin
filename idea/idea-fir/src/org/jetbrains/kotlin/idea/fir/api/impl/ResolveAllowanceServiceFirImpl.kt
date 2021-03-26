/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.api.impl

import org.jetbrains.kotlin.idea.api.ResolveAllowanceService
import org.jetbrains.kotlin.miniStdLib.multithreadings.javaThreadLocal

class ResolveAllowanceServiceFirImpl : ResolveAllowanceService() {
    private var allowResolveOnEdt by javaThreadLocal(false)
    private var forbidResolve by javaThreadLocal<String?>(null)

    override fun switchOnAllowingResolveInEdtInCurrentThread(): SwitchResult {
        if (allowResolveOnEdt) {
            return SwitchResult.ALREADY_SWITCHED
        } else {
            allowResolveOnEdt = true
            return SwitchResult.SWITCHED
        }
    }

    override fun switchOffAllowingResolveInEdtInCurrentThread() {
        allowResolveOnEdt = false
    }

    override fun isResolveOnEdtInCurrentThreadAllowed(): Boolean = allowResolveOnEdt

    override fun switchOnForbidResolveInCurrentThread(actionName: String): SwitchResult {
        if (forbidResolve != null) {
            return SwitchResult.ALREADY_SWITCHED
        } else {
            forbidResolve = actionName
            return SwitchResult.SWITCHED
        }
    }

    override fun switchOffForbidResolveInCurrentThread() {
        forbidResolve = null
    }

    override fun getResolveInCurrentThreadForbiddenReason(): String? = forbidResolve
}