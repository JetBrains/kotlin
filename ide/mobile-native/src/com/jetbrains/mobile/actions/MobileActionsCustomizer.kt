/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.jetbrains.mobile.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.impl.ActionConfigurationCustomizer
import com.jetbrains.mobile.execution.MobileBuildAction

class MobileActionsCustomizer : ActionConfigurationCustomizer {
    override fun customize(actionManager: ActionManager) {
        actionManager.replaceAction("CompileDirty", MobileBuildAction())
    }
}