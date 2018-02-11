/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class KotlinPluginUtil {

    public static final PluginId KOTLIN_PLUGIN_ID = PluginId.getId("org.jetbrains.kotlin");

    @NotNull
    public static String getPluginVersion() {
        IdeaPluginDescriptor plugin = PluginManager.getPlugin(KOTLIN_PLUGIN_ID);
        assert plugin != null : "Kotlin plugin not found: " + Arrays.toString(PluginManagerCore.getPlugins());
        return plugin.getVersion();
    }

    public static boolean isSnapshotVersion() {
        return "@snapshot@".equals(getPluginVersion());
    }
}
