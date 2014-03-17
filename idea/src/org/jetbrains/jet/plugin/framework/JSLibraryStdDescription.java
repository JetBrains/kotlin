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
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.plugin.configuration.KotlinJsModuleConfigurator;
import org.jetbrains.jet.plugin.framework.ui.CreateJavaScriptLibraryDialog;

import javax.swing.*;
import java.io.File;
import java.util.Set;

import static org.jetbrains.jet.plugin.configuration.ConfigureKotlinInProjectUtils.getConfiguratorByName;
import static org.jetbrains.jet.plugin.configuration.KotlinJsModuleConfigurator.NAME;
import static org.jetbrains.jet.plugin.configuration.KotlinJsModuleConfigurator.isJsFilePresent;
import static org.jetbrains.jet.plugin.configuration.KotlinWithLibraryConfigurator.getFileInDir;
import static org.jetbrains.jet.plugin.framework.ui.FileUIUtils.createRelativePath;

public class JSLibraryStdDescription extends CustomLibraryDescriptorWithDefferConfig {
    public static final LibraryKind KOTLIN_JAVASCRIPT_KIND = LibraryKind.create("kotlin-js-stdlib");
    public static final String LIBRARY_NAME = "KotlinJavaScript";

    public static final String JAVA_SCRIPT_LIBRARY_CREATION = "JavaScript Library Creation";
    public static final Set<LibraryKind> SUITABLE_LIBRARY_KINDS = Sets.newHashSet(KOTLIN_JAVASCRIPT_KIND);

    private static final String DEFAULT_LIB_DIR_NAME = "lib";
    private static final String DEFAULT_SCRIPT_DIR_NAME = "script";

    private final boolean useRelativePaths;
    private DeferredCopyFileRequests deferredCopyFileRequests;

    public JSLibraryStdDescription(@Nullable Project project) {
        useRelativePaths = project == null;
    }

    @NotNull
    @Override
    public Set<? extends LibraryKind> getSuitableLibraryKinds() {
        return SUITABLE_LIBRARY_KINDS;
    }

    @NotNull
    @Override
    public String getLibraryNamePrefix() {
        return LIBRARY_NAME;
    }

    @Nullable
    @Override
    public DeferredCopyFileRequests getCopyFileRequests() {
        return deferredCopyFileRequests;
    }

    @Nullable
    @Override
    public NewLibraryConfiguration createNewLibrary(@NotNull JComponent parentComponent, @Nullable VirtualFile contextDirectory) {
        KotlinJsModuleConfigurator jsConfigurator = (KotlinJsModuleConfigurator) getConfiguratorByName(NAME);
        assert jsConfigurator != null : "Cannot find configurator with name " + NAME;

        deferredCopyFileRequests = new DeferredCopyFileRequests(jsConfigurator);

        String defaultPathToJsFileDir =
                useRelativePaths ? DEFAULT_SCRIPT_DIR_NAME : createRelativePath(null, contextDirectory, DEFAULT_SCRIPT_DIR_NAME);
        String defaultPathToJarFileDir =
                useRelativePaths ? DEFAULT_LIB_DIR_NAME : createRelativePath(null, contextDirectory, DEFAULT_LIB_DIR_NAME);

        boolean jsFilePresent = isJsFilePresent(defaultPathToJsFileDir);
        boolean jarFilePresent = getFileInDir(jsConfigurator.getJarName(), defaultPathToJarFileDir).exists();

        if (jarFilePresent && jsFilePresent) {
            return createConfiguration(getFileInDir(jsConfigurator.getJarName(), defaultPathToJarFileDir));
        }

        CreateJavaScriptLibraryDialog dialog =
                new CreateJavaScriptLibraryDialog(defaultPathToJarFileDir, defaultPathToJsFileDir, !jarFilePresent, !jsFilePresent);
        dialog.show();

        if (!dialog.isOK()) return null;

        String copyJsFileIntoPath = dialog.getCopyJsIntoPath();
        if (!jsFilePresent && copyJsFileIntoPath != null) {
            deferredCopyFileRequests.addCopyRequest(jsConfigurator.getJsFile(), copyJsFileIntoPath);
        }

        if (jarFilePresent) {
            return createConfiguration(getFileInDir(jsConfigurator.getJarName(), defaultPathToJarFileDir));
        }
        else {
            String copyIntoPath = dialog.getCopyLibraryIntoPath();
            File existedJarFile = jsConfigurator.getExistedJarFile();

            if (copyIntoPath != null) {
                deferredCopyFileRequests.addCopyWithReplaceRequest(existedJarFile, copyIntoPath);
            }

            return createConfiguration(existedJarFile);
        }
    }

    @TestOnly
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
