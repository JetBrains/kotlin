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
    public static final PluginId KOTLIN_PLUGIN_ID;

    private static final String PATCHED_PLUGIN_VERSION;
    private static final String PATCHED_PLUGIN_VERSION_KEY = "kotlin.plugin.version";

    static {
        PluginId pluginId = PluginManagerCore.getPluginByClassName(KotlinPluginUtil.class.getName());
        if (pluginId == null) {
            pluginId = PluginId.getId("org.jetbrains.kotlin");
        }
        KOTLIN_PLUGIN_ID = pluginId;

        PATCHED_PLUGIN_VERSION = System.getProperty(PATCHED_PLUGIN_VERSION_KEY, null);
    }

    @NotNull
    public static String getPluginVersion() {
        if (PATCHED_PLUGIN_VERSION != null) {
            assert isPatched();
            return PATCHED_PLUGIN_VERSION;
        }

        //noinspection deprecation
        return getPluginVersionFromIdea();
    }

    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    @NotNull
    public static String getPluginVersionFromIdea() {
        IdeaPluginDescriptor plugin = PluginManager.getPlugin(KOTLIN_PLUGIN_ID);
        assert plugin != null : "Kotlin plugin not found: " + Arrays.toString(PluginManagerCore.getPlugins());
        return plugin.getVersion();
    }

    public static boolean isPatched() {
        return PATCHED_PLUGIN_VERSION != null;
    }

    public static boolean isSnapshotVersion() {
        return "@snapshot@".equals(getPluginVersion());
    }

    public static boolean isDevVersion() {
        return getPluginVersion().contains("-dev-");
    }
}
