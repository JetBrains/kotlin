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

import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.impl.scopes.LibraryScope;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.framework.JavaRuntimeLibraryDescription;
import org.jetbrains.kotlin.idea.framework.KotlinLibraryUtilKt;
import org.jetbrains.kotlin.idea.versions.KotlinRuntimeLibraryUtilKt;
import org.jetbrains.kotlin.resolve.TargetPlatform;
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform;
import org.jetbrains.kotlin.utils.KotlinPaths;
import org.jetbrains.kotlin.utils.PathUtil;

public class KotlinJavaModuleConfigurator extends KotlinWithLibraryConfigurator {
    public static final String NAME = "java";

    @Override
    public boolean isConfigured(@NotNull Module module) {
        return ConfigureKotlinInProjectUtilsKt.hasKotlinJvmRuntimeInScope(module);
    }

    @NotNull
    @Override
    protected String getLibraryName() {
        return JavaRuntimeLibraryDescription.LIBRARY_NAME;
    }

    @NotNull
    @Override
    protected String getDialogTitle() {
        return JavaRuntimeLibraryDescription.DIALOG_TITLE;
    }

    @NotNull
    @Override
    protected String getLibraryCaption() {
        return JavaRuntimeLibraryDescription.LIBRARY_CAPTION;
    }

    @NotNull
    @Override
    protected String getMessageForOverrideDialog() {
        return JavaRuntimeLibraryDescription.JAVA_RUNTIME_LIBRARY_CREATION;
    }

    @NotNull
    @Override
    public String getPresentableText() {
        return "Java";
    }

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
    public RuntimeLibraryFiles getExistingJarFiles() {
        KotlinPaths paths = PathUtil.getKotlinPathsForIdeaPlugin();
        return new RuntimeLibraryFiles(
                assertFileExists(paths.getRuntimePath()),
                assertFileExists(paths.getReflectPath()),
                assertFileExists(paths.getRuntimeSourcesPath())
        );
    }

    @Nullable
    @Override
    protected String getOldSourceRootUrl(@NotNull Library library) {
        VirtualFile runtimeJarPath = KotlinLibraryUtilKt.getRuntimeJar(library);
        return runtimeJarPath != null ? runtimeJarPath.getUrl() + "src" : null;
    }

    @Override
    protected boolean isKotlinLibrary(@NotNull Project project, @NotNull Library library) {
        if (super.isKotlinLibrary(project, library)) {
            return true;
        }

        LibraryScope scope = new LibraryScope(project, library);
        return KotlinRuntimeLibraryUtilKt.getKotlinJvmRuntimeMarkerClass(project, scope) != null;
    }

    KotlinJavaModuleConfigurator() {
    }

    public static KotlinJavaModuleConfigurator getInstance() {
        return Extensions.findExtension(Companion.getEP_NAME(), KotlinJavaModuleConfigurator.class);
    }
}
