/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.sdk;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.LibraryKind;
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration;
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.LibraryEditor;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Maxim.Manuylov
 *         Date: 19.05.12
 */
public class KotlinSdkDescription extends CustomLibraryDescription {
    @NotNull
    @Override
    public Set<? extends LibraryKind> getSuitableLibraryKinds() {
        return Collections.singleton(KotlinSdkUtil.getKotlinSdkLibraryKind());
    }

    @Nullable
    @Override
    public NewLibraryConfiguration createNewLibrary(@NotNull final JComponent parentComponent, @Nullable final VirtualFile contextDirectory) {
        VirtualFile initial = findFile(System.getenv("KOTLIN_HOME"));
        if (initial == null) {
            initial = findFile(System.getProperty("kotlinHome"));
        }

        final FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false) {
            @Override
            public boolean isFileSelectable(@Nullable final VirtualFile file) {
                return super.isFileSelectable(file) && KotlinSdkUtil.isSDKHome(file);
            }
        };
        descriptor.setTitle("Kotlin SDK");
        descriptor.setDescription("Choose a directory containing Kotlin distribution");

        final VirtualFile sdkHomeVFile = FileChooser.chooseFile(parentComponent, descriptor, initial);
        if (sdkHomeVFile == null) return null;

        final File sdkHome = new File(sdkHomeVFile.getPath());
        final String sdkVersion = KotlinSdkUtil.getSDKVersion(sdkHome);
        if (sdkVersion == null) {
            Messages.showErrorDialog(parentComponent,
                                     "Failed to find Kotlin SDK in the specified path: cannot determine Kotlin version.",
                                     "Failed to Find Kotlin SDK");
            return null;
        }

        return new NewLibraryConfiguration(KotlinSdkUtil.getSDKName(sdkHome, sdkVersion)) {
            @Override
            public void addRoots(@NotNull final LibraryEditor editor) {
                addSDKRoots(editor, sdkHome);
            }
        };
    }

    @Nullable
    private static VirtualFile findFile(@Nullable final String path) {
        if (StringUtil.isEmptyOrSpaces(path)) return null;
        //noinspection ConstantConditions
        return LocalFileSystem.getInstance().findFileByPath(FileUtil.toSystemIndependentName(path));
    }

    public static void addSDKRoots(@NotNull final LibraryEditor editor, @NotNull final File sdkHome) {
        final File libDir = new File(sdkHome, "lib");
        if (!libDir.isDirectory()) return;

        final List<File> jars = new ArrayList<File>();
        collectJars(libDir, jars);

        for (final File jar : jars) {
            editor.addRoot(VfsUtil.getUrlForLibraryRoot(jar), OrderRootType.CLASSES);
        }
    }

    private static void collectJars(@NotNull final File dir, @NotNull final List<File> jars) {
        final File[] children = dir.listFiles();
        if (children == null) return;

        for (final File child : children) {
            if (child.isDirectory()) {
                collectJars(child, jars);
            }
            else if (child.isFile() && child.getName().endsWith(".jar")) {
                jars.add(child);
            }
        }
    }

    @NotNull
    @Override
    public LibrariesContainer.LibraryLevel getDefaultLevel() {
        return LibrariesContainer.LibraryLevel.GLOBAL;
    }
}
