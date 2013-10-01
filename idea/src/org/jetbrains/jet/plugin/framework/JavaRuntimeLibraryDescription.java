/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.LibraryKind;
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration;
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription;
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

public class JavaRuntimeLibraryDescription extends CustomLibraryDescription {
    public static final LibraryKind KOTLIN_JAVA_RUNTIME_KIND = LibraryKind.create("kotlin-java-runtime");
    public static final String LIBRARY_NAME = "KotlinJavaRuntime";

    public static final String JAVA_RUNTIME_LIBRARY_CREATION = "Java Runtime Library Creation";
    private static final Set<LibraryKind> libraryKinds = Sets.newHashSet(KOTLIN_JAVA_RUNTIME_KIND);

    @NotNull
    @Override
    public Set<? extends LibraryKind> getSuitableLibraryKinds() {
        return libraryKinds;
    }

    @Nullable
    @Override
    public NewLibraryConfiguration createNewLibrary(@NotNull JComponent parentComponent, @Nullable VirtualFile contextDirectory) {
        KotlinJavaModuleConfigurator configurator = (KotlinJavaModuleConfigurator) ConfigureKotlinInProjectUtils
                .getConfiguratorByName(KotlinJavaModuleConfigurator.NAME);
        assert configurator != null : "Configurator with name " + KotlinJavaModuleConfigurator.NAME + " should exists";

        String defaultPathToJarFile = FileUIUtils.createRelativePath(null, contextDirectory, "lib");

        boolean jarFilePresent = getFileInDir(configurator.getJarName(), defaultPathToJarFile).exists();

        File libraryFile;
        File librarySrcFile;
        if (jarFilePresent) {
            libraryFile = getFileInDir(configurator.getJarName(), defaultPathToJarFile);
            File sourcesJar = getFileInDir(configurator.getSourcesJarName(), defaultPathToJarFile);
            librarySrcFile = sourcesJar.exists() ? sourcesJar
                                                 : configurator.copyFileToDir(configurator.getExistedSourcesJarFile(), libraryFile.getParent());
        }
        else {
            CreateJavaLibraryDialog dialog = new CreateJavaLibraryDialog(defaultPathToJarFile);
            dialog.show();

            if (!dialog.isOK()) return null;

            String copyIntoPath = dialog.getCopyIntoPath();

            File existedJarFile = configurator.getExistedJarFile();
            libraryFile = copyIntoPath != null ? configurator.copyFileToDir(existedJarFile, copyIntoPath) : existedJarFile;

            File existedSourcesJarFile = configurator.getExistedSourcesJarFile();
            librarySrcFile = copyIntoPath != null ? configurator.copyFileToDir(existedSourcesJarFile, copyIntoPath) : existedSourcesJarFile;
        }

        final String libraryFileUrl = VfsUtil.getUrlForLibraryRoot(libraryFile);
        final String libraryFileSrcUrl = VfsUtil.getUrlForLibraryRoot(librarySrcFile);

        return new NewLibraryConfiguration(LIBRARY_NAME, getDownloadableLibraryType(), new LibraryVersionProperties()) {
            @Override
            public void addRoots(@NotNull LibraryEditor editor) {
                editor.addRoot(libraryFileUrl, OrderRootType.CLASSES);
                editor.addRoot(libraryFileSrcUrl, OrderRootType.SOURCES);
            }
        };
    }
}