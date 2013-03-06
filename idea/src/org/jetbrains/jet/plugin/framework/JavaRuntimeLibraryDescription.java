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
import org.jetbrains.jet.plugin.framework.ui.CreateJavaLibraryDialog;
import org.jetbrains.jet.plugin.framework.ui.FileUIUtils;
import org.jetbrains.jet.utils.KotlinPaths;
import org.jetbrains.jet.utils.PathUtil;

import javax.swing.*;
import java.io.File;
import java.util.Set;

public class JavaRuntimeLibraryDescription extends CustomLibraryDescription {
    public static final LibraryKind KOTLIN_JAVA_RUNTIME_KIND = LibraryKind.create("kotlin-java-runtime");
    public static final String LIBRARY_NAME = "KotlinJavaRuntime";

    private static final String JAVA_RUNTIME_LIBRARY_CREATION = "Java Runtime Library Creation";
    private static final Set<LibraryKind> libraryKinds = Sets.newHashSet(KOTLIN_JAVA_RUNTIME_KIND);

    @NotNull
    @Override
    public Set<? extends LibraryKind> getSuitableLibraryKinds() {
        return libraryKinds;
    }

    @Nullable
    @Override
    public NewLibraryConfiguration createNewLibrary(@NotNull JComponent parentComponent, @Nullable VirtualFile contextDirectory) {
        CreateJavaLibraryDialog dialog = new CreateJavaLibraryDialog(null, "Create Kotlin Java Runtime Library", contextDirectory);
        dialog.show();

        if (!dialog.isOK()) return null;

        KotlinPaths paths = PathUtil.getKotlinPathsForIdeaPlugin();

        File libraryFile = paths.getRuntimePath();
        if (!libraryFile.exists()) {
            Messages.showErrorDialog(
                    parentComponent,
                    String.format("Java Runtime library was not found in '%s'." , paths.getLibPath()),
                    JAVA_RUNTIME_LIBRARY_CREATION);
            return null;
        }

        String copyIntoPath = dialog.getCopyIntoPath();
        if (copyIntoPath != null) {
            libraryFile = FileUIUtils.copyWithOverwriteDialog(parentComponent, JAVA_RUNTIME_LIBRARY_CREATION, copyIntoPath, libraryFile);
            if (libraryFile == null) {
                return null;
            }
        }

        final String libraryFileUrl = VfsUtil.getUrlForLibraryRoot(libraryFile);
        return new NewLibraryConfiguration(LIBRARY_NAME, getDownloadableLibraryType(), new LibraryVersionProperties()) {
            @Override
            public void addRoots(@NotNull LibraryEditor editor) {
                editor.addRoot(libraryFileUrl, OrderRootType.CLASSES);
                editor.addRoot(libraryFileUrl + "src", OrderRootType.SOURCES);
            }
        };
    }
}