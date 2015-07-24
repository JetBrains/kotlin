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
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.dom.model.MavenDomPlugin;
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil;
import org.jetbrains.kotlin.resolve.TargetPlatform;
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform;

public class KotlinJavaMavenConfigurator extends KotlinMavenConfigurator {
    private static final String NAME = "maven";
    private static final String STD_LIB_ID = "kotlin-stdlib";
    private static final String PRESENTABLE_TEXT = "Maven";

    public KotlinJavaMavenConfigurator() {
        super(STD_LIB_ID, NAME, PRESENTABLE_TEXT);
    }

    @Override
    protected boolean isKotlinModule(@NotNull Module module) {
        return ProjectStructureUtil.isJavaKotlinModule(module);
    }

    @Override
    protected void createExecutions(VirtualFile virtualFile, MavenDomPlugin kotlinPlugin, Module module) {
        createExecution(virtualFile, kotlinPlugin, module, false);
        createExecution(virtualFile, kotlinPlugin, module, true);
    }

    @NotNull
    @Override
    public TargetPlatform getTargetPlatform() {
        return JvmPlatform.INSTANCE$;
    }
}
