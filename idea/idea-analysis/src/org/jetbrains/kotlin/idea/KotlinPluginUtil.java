/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.configuration.KotlinModuleTypeManager;

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

    public static boolean isAndroidGradleModule(@NotNull Module module) {
        return KotlinModuleTypeManager.getInstance().isAndroidGradleModule(module);
    }

    public static boolean isGradleModule(@NotNull Module module) {
        return KotlinModuleTypeManager.getInstance().isGradleModule(module);
    }

    public static boolean isMavenModule(@NotNull Module module) {
        // This constant could be acquired from MavenProjectsManager, but we don't want to depend on the Maven plugin...
        // See MavenProjectsManager.isMavenizedModule()
        return "true".equals(module.getOptionValue("org.jetbrains.idea.maven.project.MavenProjectsManager.isMavenModule"));
    }
}
