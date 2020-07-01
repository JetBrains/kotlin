package com.jetbrains.mobile.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.impl.ActionConfigurationCustomizer
import com.jetbrains.mobile.execution.MobileBuildAction

class MobileActionsCustomizer : ActionConfigurationCustomizer {
    override fun customize(actionManager: ActionManager) {
        actionManager.replaceAction("CompileDirty", MobileBuildAction())
    }
}