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

package org.jetbrains.kotlin.idea.maven.configuration;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.dom.model.MavenDomPlugin;
import org.jetbrains.kotlin.idea.maven.PomFile;
import org.jetbrains.kotlin.js.resolve.JsPlatform;
import org.jetbrains.kotlin.resolve.TargetPlatform;

public class KotlinJavascriptMavenConfigurator extends KotlinMavenConfigurator {
    private static final String NAME = "js maven";
    public static final String STD_LIB_ID = "kotlin-js-library";
    private static final String JS_GOAL = "js";
    private static final String JS_TEST_GOAL = "test-js";
    private static final String JS_EXECUTION_ID = "js";
    private static final String PRESENTABLE_TEXT = "JavaScript Maven - experimental";

    public KotlinJavascriptMavenConfigurator() {
        super(STD_LIB_ID, null, false, NAME, PRESENTABLE_TEXT);
    }

    @Override
    protected boolean isKotlinModule(@NotNull Module module) {
        return ConfigureKotlinInProjectUtilsKt.hasKotlinJsRuntimeInScope(module);
    }

    @Override
    protected boolean isRelevantGoal(@NotNull String goalName) {
        return goalName.equals(PomFile.KotlinGoals.INSTANCE.getJs());
    }

    @Override
    protected void createExecutions(@NotNull PomFile pomFile, @NotNull MavenDomPlugin kotlinPlugin, @NotNull Module module) {
        createExecution(pomFile, kotlinPlugin, module, false);
        createExecution(pomFile, kotlinPlugin, module, true);
    }

    @NotNull
    @Override
    protected String getExecutionId(boolean isTest) {
        return JS_EXECUTION_ID + (isTest ? "-test" : "");
    }

    @NotNull
    @Override
    protected String getGoal(boolean isTest) {
        return isTest ? JS_TEST_GOAL : JS_GOAL;
    }

    @NotNull
    @Override
    public TargetPlatform getTargetPlatform() {
        return JsPlatform.INSTANCE;
    }
}
