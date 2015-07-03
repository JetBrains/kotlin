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

import com.intellij.openapi.application.PathMacros;
import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.debugger.filter.FilterPackage;
import org.jetbrains.kotlin.utils.PathUtil;

public class PluginStartupComponent implements ApplicationComponent {
    private static final String KOTLIN_BUNDLED = "KOTLIN_BUNDLED";

    @Override
    @NotNull
    public String getComponentName() {
        return PluginStartupComponent.class.getName();
    }

    @Override
    public void initComponent() {
        registerPathVariable();

        FilterPackage.addKotlinStdlibDebugFilterIfNeeded();
    }

    private static void registerPathVariable() {
        PathMacros macros = PathMacros.getInstance();
        macros.setMacro(KOTLIN_BUNDLED, PathUtil.getKotlinPathsForIdeaPlugin().getHomePath().getPath());
    }

    @Override
    public void disposeComponent() {
    }
}
