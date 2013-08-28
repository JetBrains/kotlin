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
import org.jetbrains.jet.plugin.configuration.KotlinJsModuleConfigurator;
import org.jetbrains.jet.plugin.framework.ui.CreateJavaScriptLibraryDialog;
import org.jetbrains.jet.plugin.framework.ui.FileUIUtils;

import javax.swing.*;
import java.io.File;
import java.util.Set;

import static org.jetbrains.jet.plugin.configuration.ConfigureKotlinInProjectUtils.getConfiguratorByName;
import static org.jetbrains.jet.plugin.configuration.KotlinJsModuleConfigurator.NAME;
import static org.jetbrains.jet.plugin.framework.ui.FileUIUtils.createRelativePath;

public class JSLibraryStdDescription extends CustomLibraryDescription {
    public static final LibraryKind KOTLIN_JAVASCRIPT_KIND = LibraryKind.create("kotlin-js-stdlib");
    public static final String LIBRARY_NAME = "KotlinJavaScript";

    public static final String JAVA_SCRIPT_LIBRARY_CREATION = "JavaScript Library Creation";
    private static final Set<LibraryKind> libraryKinds = Sets.newHashSet(KOTLIN_JAVASCRIPT_KIND);

    @NotNull
    @Override
    public Set<? extends LibraryKind> getSuitableLibraryKinds() {
        return libraryKinds;
    }

    @Nullable
    @Override
    public NewLibraryConfiguration createNewLibrary(@NotNull JComponent parentComponent, @Nullable VirtualFile contextDirectory) {
        KotlinJsModuleConfigurator configurator = (KotlinJsModuleConfigurator) getConfiguratorByName(NAME);
        assert configurator != null : "Cannot find configurator with name " + NAME;

        String defaultPathToJsFileDir = createRelativePath(null, contextDirectory, "script");
        String defaultPathToJarFileDir = createRelativePath(null, contextDirectory, "lib");

        boolean jsFilePresent = KotlinJsModuleConfigurator.isJsFilePresent(defaultPathToJsFileDir);
        boolean jarFilePresent = configurator.isJarPresent(defaultPathToJarFileDir);

        if (jarFilePresent && jsFilePresent) {
            return createConfiguration(configurator.getJarInDir(defaultPathToJarFileDir));
        }

        CreateJavaScriptLibraryDialog dialog =
                new CreateJavaScriptLibraryDialog(defaultPathToJarFileDir, defaultPathToJsFileDir, !jarFilePresent, !jsFilePresent);
        dialog.show();

        if (!dialog.isOK()) return null;

        String copyJsFileIntoPath = dialog.getCopyJsIntoPath();
        if (!jsFilePresent && copyJsFileIntoPath != null) {
            configurator.copyFileToDir(configurator.getJsFile(), copyJsFileIntoPath);
        }

        if (jarFilePresent) {
            return createConfiguration(configurator.getJarInDir(defaultPathToJarFileDir));
        }
        else {
            String copyIntoPath = dialog.getCopyLibraryIntoPath();
            if (copyIntoPath != null) {
                return createConfiguration(configurator.copyJarToDir(copyIntoPath));
            }
            else {
                return createConfiguration(configurator.getExistedJarFile());
            }
        }
    }

    public NewLibraryConfiguration createNewLibraryForTests() {
        KotlinJsModuleConfigurator configurator = (KotlinJsModuleConfigurator) getConfiguratorByName(NAME);
        assert configurator != null : "Cannot find configurator with name " + NAME;

        return createConfiguration(configurator.getExistedJarFile());
    }

    private NewLibraryConfiguration createConfiguration(@NotNull File libraryFile) {
        final String libraryRoot = VfsUtil.getUrlForLibraryRoot(libraryFile);
        return new NewLibraryConfiguration(LIBRARY_NAME, getDownloadableLibraryType(), new LibraryVersionProperties()) {
            @Override
            public void addRoots(@NotNull LibraryEditor editor) {
                editor.addRoot(libraryRoot, OrderRootType.CLASSES);
                editor.addRoot(libraryRoot, OrderRootType.SOURCES);
            }
        };
    }
}
