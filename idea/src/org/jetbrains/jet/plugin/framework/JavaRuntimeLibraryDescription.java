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
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetPluginUtil;
import org.jetbrains.jet.plugin.framework.ui.FileUIUtils;
import org.jetbrains.jet.utils.PathUtil;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Set;

public class JavaRuntimeLibraryDescription extends CustomLibraryDescription {
    public static final LibraryKind KOTLIN_JAVA_RUNTIME_KIND = LibraryKind.create("kotlin-java-runtime");
    public static final String LIBRARY_NAME = "KotlinJavaRuntime";

    private static final String JAVA_RUNTIME_LIBRARY_CREATION = "Java Runtime Library Creation";

    @NotNull
    @Override
    public Set<? extends LibraryKind> getSuitableLibraryKinds() {
        return Sets.newHashSet(KOTLIN_JAVA_RUNTIME_KIND);
    }

    @Nullable
    @Override
    public NewLibraryConfiguration createNewLibrary(@NotNull JComponent parentComponent, @Nullable VirtualFile contextDirectory) {
        return createFromPlugin(contextDirectory);
    }

    private NewLibraryConfiguration createFromPlugin(VirtualFile contextDirectory) {
        File runtimePath = PathUtil.getKotlinPathsForIdeaPlugin().getRuntimePath();

        if (!runtimePath.exists()) {
            Messages.showErrorDialog("Java Runtime library was not found. Make sure plugin is installed properly.",
                                     JAVA_RUNTIME_LIBRARY_CREATION);
            return null;
        }

        String directoryPath = FileUIUtils.selectDestinationFolderDialog(
                null, contextDirectory, "Select folder where bundled Kotlin java runtime library should be copied");

        if (directoryPath == null) {
            return null;
        }

        final File targetFile;
        try {
            targetFile = FileUIUtils.copyWithOverwriteDialog(directoryPath, runtimePath);
        }
        catch (IOException e) {
            Messages.showErrorDialog("Error during file copy", JAVA_RUNTIME_LIBRARY_CREATION);
            return null;
        }

        return new NewLibraryConfiguration(LIBRARY_NAME + "-" + JetPluginUtil.getPluginVersion(), getDownloadableLibraryType(), new LibraryVersionProperties()) {
            @Override
            public void addRoots(@NotNull LibraryEditor editor) {
                editor.addRoot(VfsUtil.getUrlForLibraryRoot(targetFile), OrderRootType.CLASSES);
                editor.addRoot(VfsUtil.getUrlForLibraryRoot(targetFile) + "src", OrderRootType.SOURCES);
            }
        };
    }
}