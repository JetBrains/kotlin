/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.impl.scopes.LibraryScope;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.framework.JavaRuntimeLibraryDescription;
import org.jetbrains.jet.plugin.framework.JavaRuntimePresentationProvider;
import org.jetbrains.jet.plugin.framework.ui.CreateJavaLibraryDialogWithModules;
import org.jetbrains.jet.plugin.project.ProjectStructureUtil;
import org.jetbrains.jet.plugin.versions.KotlinRuntimeLibraryCoreUtil;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.jetbrains.jet.plugin.configuration.ConfigureKotlinInProjectUtils.showInfoNotification;

public class KotlinJavaModuleConfigurator extends KotlinWithLibraryConfigurator {
    public static final String NAME = "java";

    @Override
    public boolean isConfigured(@NotNull Module module) {
        return ProjectStructureUtil.isJavaKotlinModule(module);
    }

    @NotNull
    @Override
    protected String getLibraryName() {
        return JavaRuntimeLibraryDescription.LIBRARY_NAME;
    }

    @NotNull
    @Override
    public String getJarName() {
        return PathUtil.KOTLIN_JAVA_RUNTIME_JAR;
    }

    @NotNull
    @Override
    public String getSourcesJarName() {
        return PathUtil.KOTLIN_JAVA_RUNTIME_SRC_JAR;
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

    @Override
    public void configure(@NotNull Project project) {
        String defaultPath = getDefaultPathToJarFile(project);
        boolean showPathPanelForJava = needToChooseJarPath(project);

        List<Module> nonConfiguredModules = !ApplicationManager.getApplication().isUnitTestMode() ?
                ConfigureKotlinInProjectUtils.getNonConfiguredModules(project, this):
                Arrays.asList(ModuleManager.getInstance(project).getModules());

        List<Module> modulesToConfigure = nonConfiguredModules;
        String copyLibIntoPath = null;

        if (nonConfiguredModules.size() > 1 || showPathPanelForJava) {
            CreateJavaLibraryDialogWithModules dialog = new CreateJavaLibraryDialogWithModules(
                    project, nonConfiguredModules, defaultPath, showPathPanelForJava);

            if (!ApplicationManager.getApplication().isUnitTestMode()) {
                dialog.show();
                if (!dialog.isOK()) return;
            }

            modulesToConfigure = dialog.getModulesToConfigure();
            copyLibIntoPath = dialog.getCopyIntoPath();
        }

        for (Module module : modulesToConfigure) {
            configureModuleWithLibrary(module, defaultPath, copyLibIntoPath);
        }
    }

    @Override
    @NotNull
    public File getExistedJarFile() {
        return assertFileExists(PathUtil.getKotlinPathsForIdeaPlugin().getRuntimePath());
    }

    @Override
    public File getExistedSourcesJarFile() {
        return assertFileExists(PathUtil.getKotlinPathsForIdeaPlugin().getRuntimeSourcesPath());
    }

    public void copySourcesToPathFromLibrary(@NotNull Library library) {
        String dirToJarFromLibrary = getPathFromLibrary(library, OrderRootType.SOURCES);
        assert dirToJarFromLibrary != null : "Directory to file from library should be non null";

        copyFileToDir(getExistedSourcesJarFile(), dirToJarFromLibrary);
    }

    public boolean changeOldSourcesPathIfNeeded(@NotNull Library library) {
        if (!removeOldSourcesRootIfNeeded(library)) {
            return false;
        }

        String parentDir = getPathFromLibrary(library, OrderRootType.CLASSES);
        assert parentDir != null : "Parent dir for classes jar should exists for Kotlin library";

        return addSourcesToLibraryIfNeeded(library, getFileInDir(getSourcesJarName(), parentDir));
    }

    private static boolean removeOldSourcesRootIfNeeded(@NotNull Library library) {
        VirtualFile runtimeJarPath = JavaRuntimePresentationProvider.getRuntimeJar(library);
        if (runtimeJarPath == null) {
            return false;
        }

        String oldLibrarySourceRoot = runtimeJarPath.getUrl() + "src";

        String[] librarySourceRoots = library.getUrls(OrderRootType.SOURCES);
        for (String sourceRoot : librarySourceRoots) {
            if (sourceRoot.equals(oldLibrarySourceRoot)) {
                final Library.ModifiableModel model = library.getModifiableModel();
                model.removeRoot(oldLibrarySourceRoot, OrderRootType.SOURCES);
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        model.commit();
                    }
                });

                showInfoNotification("Source root '" + oldLibrarySourceRoot + "' was removed for " + library.getName() + " library");
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isKotlinLibrary(@NotNull Project project, @NotNull Library library) {
        if (super.isKotlinLibrary(project, library)) {
            return true;
        }

        LibraryScope scope = new LibraryScope(project, library);
        return KotlinRuntimeLibraryCoreUtil.getKotlinRuntimeMarkerClass(scope) != null;
    }

    KotlinJavaModuleConfigurator() {
    }
}
