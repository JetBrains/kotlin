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

package com.intellij.ide.plugins;

import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Set;

// TODO drop this temporary hack to access to PluginManagerCore methods when CoreApplicationEnvironment got similar features.
public class PluginManagerCoreProxy {
    private PluginManagerCoreProxy() {}

    @Nullable
    public static IdeaPluginDescriptorImpl loadDescriptorFromDir(@NotNull File file, @NotNull String fileName) {
        return PluginManagerCore.loadDescriptorFromDir(file, fileName);
    }

    @Nullable
    public static IdeaPluginDescriptorImpl loadDescriptorFromJar(@NotNull File file, @NotNull String fileName) {
        return PluginManagerCore.loadDescriptorFromJar(file, fileName);
    }

    // copied as is from PluginManagerCore#registerExtensionPointsAndExtensions
    public static void registerExtensionPointsAndExtensions(ExtensionsArea area, List<IdeaPluginDescriptorImpl> loadedPlugins) {
        for (IdeaPluginDescriptorImpl descriptor : loadedPlugins) {
            descriptor.registerExtensionPoints(area);
        }

        Set<String> epNames = ContainerUtil.newHashSet();
        for (ExtensionPoint point : area.getExtensionPoints()) {
            epNames.add(point.getName());
        }

        for (IdeaPluginDescriptorImpl descriptor : loadedPlugins) {
            for (String epName : epNames) {
                descriptor.registerExtensions(area, epName);
            }
        }
    }
}
