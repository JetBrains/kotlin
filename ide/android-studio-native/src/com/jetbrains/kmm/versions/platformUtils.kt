package com.jetbrains.kmm.versions

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId

internal fun disablePlugin(id: String) {
    PluginManagerCore.disablePlugin(PluginId.getId("com.jetbrains.kmm"))
}