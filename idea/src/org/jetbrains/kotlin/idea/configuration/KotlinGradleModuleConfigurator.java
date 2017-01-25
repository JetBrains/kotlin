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

package org.jetbrains.kotlin.idea.configuration;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinPluginUtil;
import org.jetbrains.kotlin.resolve.TargetPlatform;
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

public class KotlinGradleModuleConfigurator extends KotlinWithGradleConfigurator {
    public static final String NAME = "gradle";

    public static final String APPLY_KOTLIN = "apply plugin: 'kotlin'";

    @NotNull
    @Override
    public String getName() {
        return NAME;
    }

    @NotNull
    @Override
    public TargetPlatform getTargetPlatform() {
        return JvmPlatform.INSTANCE;
    }

    @NotNull
    @Override
    public String getPresentableText() {
        return "Gradle";
    }

    @Override
    public boolean isApplicable(@NotNull Module module) {
        return KotlinPluginUtil.isGradleModule(module) &&
               !KotlinPluginUtil.isAndroidGradleModule(module);
    }

    @Override
    protected String getApplyPluginDirective() {
        return APPLY_KOTLIN;
    }

    @Override
    protected boolean addElementsToFile(@NotNull GroovyFile groovyFile, boolean isTopLevelProjectFile, @NotNull String version) {
        if (!isTopLevelProjectFile) {
            boolean wasModified = Companion.addElementsToProjectFile(groovyFile, version);
            wasModified |= addElementsToModuleFile(groovyFile, version);
            return wasModified;
        }
        return false;
    }

    KotlinGradleModuleConfigurator() {
    }
}
