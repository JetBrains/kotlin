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

package org.jetbrains.kotlin.idea.compiler.configuration;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments;

import static org.jetbrains.kotlin.config.SettingConstants.KOTLIN_COMMON_COMPILER_ARGUMENTS_SECTION;
import static org.jetbrains.kotlin.config.SettingConstants.KOTLIN_COMPILER_SETTINGS_PATH;

@State(
    name = KOTLIN_COMMON_COMPILER_ARGUMENTS_SECTION,
    storages = {
        @Storage(file = StoragePathMacros.PROJECT_FILE),
        @Storage(file = KOTLIN_COMPILER_SETTINGS_PATH, scheme = StorageScheme.DIRECTORY_BASED)
    }
)
public class KotlinCommonCompilerArgumentsHolder extends BaseKotlinCompilerSettings<CommonCompilerArguments> {

    @NotNull
    @Override
    protected CommonCompilerArguments createSettings() {
        return new CommonCompilerArguments.DummyImpl();
    }

    public static KotlinCommonCompilerArgumentsHolder getInstance(Project project) {
        return ServiceManager.getService(project, KotlinCommonCompilerArgumentsHolder.class);
    }
}
