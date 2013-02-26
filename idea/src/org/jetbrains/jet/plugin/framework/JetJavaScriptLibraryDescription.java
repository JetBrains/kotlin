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
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.LibraryKind;
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration;
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.LibraryEditor;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.framework.ui.FrameworkSourcePanel;
import org.jetbrains.jet.plugin.versions.KotlinRuntimeLibraryUtil;
import org.jetbrains.jet.utils.PathUtil;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Set;

public class JetJavaScriptLibraryDescription extends CustomLibraryDescription {
    public static final LibraryKind KOTLIN_JAVASCRIPT_KIND = LibraryKind.create("kotlin-js-stdlib");

    private final FrameworkSourcePanel configurationPanel;

    public JetJavaScriptLibraryDescription(FrameworkSourcePanel configurationPanel) {
        this.configurationPanel = configurationPanel;
    }

    @NotNull
    @Override
    public Set<? extends LibraryKind> getSuitableLibraryKinds() {
        return Sets.newHashSet(KOTLIN_JAVASCRIPT_KIND);
    }

    @Nullable
    @Override
    public NewLibraryConfiguration createNewLibrary(@NotNull JComponent parentComponent, @Nullable VirtualFile contextDirectory) {
        if (configurationPanel.isConfigureFromBundled()) {
            // Select folder where to copy bundled library
            final FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);
            descriptor.setTitle("Select Folder");
            descriptor.setDescription("Select folder where bundled runtime should be placed");

            final VirtualFile[] files = FileChooser.chooseFiles(descriptor, parentComponent, null, contextDirectory);
            if (files.length == 0) {
                return null;
            }

            assert files.length == 1: "Only one folder is expected";

            final VirtualFile directory = files[0];
            assert directory.isDirectory();

            File runtimePath = PathUtil.getKotlinPathsForIdeaPlugin().getRuntimePath();
            if (!runtimePath.exists()) {
                return null;
            }

            File targetJar = new File(com.intellij.util.PathUtil.getLocalPath(directory), KotlinRuntimeLibraryUtil.KOTLIN_RUNTIME_JAR);
            try {
                FileUtil.copy(runtimePath, targetJar);
                VirtualFile jarVfs = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetJar);
                if (jarVfs != null) {
                    jarVfs.refresh(false, false);
                }
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }

            return new NewLibraryConfiguration(KotlinRuntimeLibraryUtil.LIBRARY_NAME, getDownloadableLibraryType(), new LibraryVersionProperties()) {
                @Override
                public void addRoots(@NotNull LibraryEditor editor) {
                    editor.addRoot(directory.getUrl() + "/" + KotlinRuntimeLibraryUtil.KOTLIN_RUNTIME_JAR, OrderRootType.CLASSES);
                }
            };
        }
        else {
            throw new IllegalStateException("Feature isn't ready yet");
        }
   }
}
