/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.util;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.sdk.KotlinSdkUtil;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;

/**
 * @author Maxim.Manuylov
 *         Date: 20.05.12
 */
public class PluginPathUtil {
    private PluginPathUtil() {}

    @Nullable
    public static File getBundledSDKHome() {
        File plugin_jar_path = new File(PathUtil.getJarPathForClass(PathUtil.class));
        if (!plugin_jar_path.exists()) return null;

        if (plugin_jar_path.getName().equals("kotlin-plugin.jar")) {
            File lib = plugin_jar_path.getParentFile();
            File pluginHome = lib.getParentFile();

            File answer = new File(pluginHome, "kotlinc");

            return answer.exists() ? answer : null;
        }

        return null;
    }

    @Nullable
    public static File getRuntimePath(@NotNull Module module) {
        return PathUtil.getRuntimePath(KotlinSdkUtil.getSDKHomeFor(module));
    }

    @Nullable
    public static File getJsLibJsPath(@NotNull Module module) {
        return PathUtil.getJsLibJsPath(KotlinSdkUtil.getSDKHomeFor(module));
    }

    @Nullable
    public static File getJsLibJarPath(@NotNull Module module) {
        return PathUtil.getJsLibJarPath(KotlinSdkUtil.getSDKHomeFor(module));
    }

    @Nullable
    public static File getJdkAnnotationsPath(@NotNull Module module) {
        return PathUtil.getJdkAnnotationsPath(KotlinSdkUtil.getSDKHomeFor(module));
    }
}
