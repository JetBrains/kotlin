/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.api.fe10.impl

import org.jetbrains.kotlin.idea.api.ResolveAllowanceService

class ResolveAllowanceServiceFE10Impl: ResolveAllowanceService() {
    override fun switchOnAllowingResolveInEdtInCurrentThread(): SwitchResult {
        return SwitchResult.ALREADY_SWITCHED
    }

    override fun isResolveOnEdtInCurrentThreadAllowed(): Boolean = true

    override fun switchOffAllowingResolveInEdtInCurrentThread() {}

    override fun switchOnForbidResolveInCurrentThread(actionName: String): SwitchResult {
        return SwitchResult.ALREADY_SWITCHED
    }

    override fun switchOffForbidResolveInCurrentThread() {
    }

    override fun getResolveInCurrentThreadForbiddenReason(): String? = null
}