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

package org.jetbrains.jet.plugin.framework;

import com.google.common.collect.Sets;
import com.intellij.framework.library.LibraryVersionProperties;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.LibraryKind;
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.LibraryEditor;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.configuration.ConfigureKotlinInProjectUtils;
import org.jetbrains.jet.plugin.configuration.KotlinJavaModuleConfigurator;
import org.jetbrains.jet.plugin.framework.ui.CreateJavaLibraryDialog;
import org.jetbrains.jet.plugin.framework.ui.FileUIUtils;

import javax.swing.*;
import java.io.File;
import java.util.Set;

import static org.jetbrains.jet.plugin.configuration.KotlinWithLibraryConfigurator.getFileInDir;

public class JavaRuntimeLibraryDescription extends CustomLibraryDescriptorWithDefferConfig {
    public static final LibraryKind KOTLIN_JAVA_RUNTIME_KIND = LibraryKind.create("kotlin-java-runtime");
    public static final String LIBRARY_NAME = "KotlinJavaRuntime";

    public static final String JAVA_RUNTIME_LIBRARY_CREATION = "Java Runtime Library Creation";
    public static final Set<LibraryKind> SUITABLE_LIBRARY_KINDS = Sets.newHashSet(KOTLIN_JAVA_RUNTIME_KIND);

    private static final String DEFAULT_LIB_DIR_NAME = "lib";

    private final boolean useRelativePaths;

    private DeferredCopyFileRequests deferredCopyFileRequests = null;

    /**
     * @param project null when project doesn't exist yet (called from project wizard)
     */
    public JavaRuntimeLibraryDescription(@Nullable Project project) {
        useRelativePaths = project == null;
    }

    @NotNull
    @Override
    public Set<? extends LibraryKind> getSuitableLibraryKinds() {
        return SUITABLE_LIBRARY_KINDS;
    }

    @NotNull
    @Override
    public LibraryKind getLibraryKind() {
        return KOTLIN_JAVA_RUNTIME_KIND;
    }

    @Nullable
    @Override
    public DeferredCopyFileRequests getCopyFileRequests() {
        return deferredCopyFileRequests;
    }

    @Nullable
    @Override
    public NewLibraryConfiguration createNewLibrary(@NotNull JComponent parentComponent, @Nullable VirtualFile contextDirectory) {
        KotlinJavaModuleConfigurator jvmConfigurator =
                (KotlinJavaModuleConfigurator) ConfigureKotlinInProjectUtils.getConfiguratorByName(KotlinJavaModuleConfigurator.NAME);
        assert jvmConfigurator != null : "Configurator with name " + KotlinJavaModuleConfigurator.NAME + " should exists";

        deferredCopyFileRequests = new DeferredCopyFileRequests(jvmConfigurator);

        String defaultPathToJarFile = useRelativePaths ? DEFAULT_LIB_DIR_NAME
                                                       : FileUIUtils.createRelativePath(null, contextDirectory, DEFAULT_LIB_DIR_NAME);

        File bundledLibJarFile = jvmConfigurator.getExistedJarFile();
        File bundledLibSourcesJarFile = jvmConfigurator.getExistedSourcesJarFile();

        File libraryFile;
        File librarySrcFile;

        File stdJarInDefaultPath = getFileInDir(jvmConfigurator.getJarName(), defaultPathToJarFile);
        if (!useRelativePaths && stdJarInDefaultPath.exists()) {
            libraryFile = stdJarInDefaultPath;

            File sourcesJar = getFileInDir(jvmConfigurator.getSourcesJarName(), defaultPathToJarFile);
            if (sourcesJar.exists()) {
                librarySrcFile = sourcesJar;
            }
            else {
                deferredCopyFileRequests.addCopyWithReplaceRequest(bundledLibSourcesJarFile, libraryFile.getParent());
                librarySrcFile = bundledLibSourcesJarFile;
            }
        }
        else {
            CreateJavaLibraryDialog dialog = new CreateJavaLibraryDialog(defaultPathToJarFile);
            dialog.show();

            if (!dialog.isOK()) return null;

            String copyIntoPath = dialog.getCopyIntoPath();
            if (copyIntoPath != null) {
                deferredCopyFileRequests.addCopyWithReplaceRequest(bundledLibJarFile, copyIntoPath);
                deferredCopyFileRequests.addCopyWithReplaceRequest(bundledLibSourcesJarFile, copyIntoPath);
            }

            libraryFile = bundledLibJarFile;
            librarySrcFile = bundledLibSourcesJarFile;
        }

        final String libraryFileUrl = VfsUtil.getUrlForLibraryRoot(libraryFile);
        final String libraryFileSrcUrl = VfsUtil.getUrlForLibraryRoot(librarySrcFile);

        return new NewLibraryConfiguration(LIBRARY_NAME, null, new LibraryVersionProperties()) {
            @Override
            public void addRoots(@NotNull LibraryEditor editor) {
                editor.addRoot(libraryFileUrl, OrderRootType.CLASSES);
                editor.addRoot(libraryFileSrcUrl, OrderRootType.SOURCES);
            }
        };
    }
}