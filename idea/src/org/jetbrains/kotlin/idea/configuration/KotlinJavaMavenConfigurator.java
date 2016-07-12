/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.configuration;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.dom.model.MavenDomPlugin;
import org.jetbrains.kotlin.resolve.TargetPlatform;
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform;

public class KotlinJavaMavenConfigurator extends KotlinMavenConfigurator {
    private static final String NAME = "maven";
    private static final String STD_LIB_ID = "kotlin-stdlib";
    private static final String TEST_LIB_ID = "kotlin-test-junit";
    private static final String PRESENTABLE_TEXT = "Maven";

    public KotlinJavaMavenConfigurator() {
        super(STD_LIB_ID, TEST_LIB_ID, true, NAME, PRESENTABLE_TEXT);
    }

    @Override
    protected boolean isKotlinModule(@NotNull Module module) {
        return ConfigureKotlinInProjectUtilsKt.hasKotlinJvmRuntimeInScope(module);
    }

    @Override
    protected void createExecutions(@NotNull PomFile pomFile, @NotNull MavenDomPlugin kotlinPlugin, @NotNull Module module) {
        createExecution(pomFile, kotlinPlugin, module, false);
        createExecution(pomFile, kotlinPlugin, module, true);
    }

    @NotNull
    @Override
    public TargetPlatform getTargetPlatform() {
        return JvmPlatform.INSTANCE;
    }
}
