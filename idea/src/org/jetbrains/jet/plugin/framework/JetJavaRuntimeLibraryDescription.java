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
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.framework.ui.FrameworkSourcePanel;
import org.jetbrains.jet.plugin.versions.KotlinRuntimeLibraryUtil;

import javax.management.openmbean.InvalidOpenTypeException;
import javax.swing.*;
import java.util.Set;

public class JetJavaRuntimeLibraryDescription extends CustomLibraryDescription {
    public static final LibraryKind KOTLIN_JAVA_RUNTIME_KIND = LibraryKind.create("kotlin-java-runtime");
    private final FrameworkSourcePanel frameworkSourcePanel;

    public JetJavaRuntimeLibraryDescription(FrameworkSourcePanel frameworkSourcePanel) {
        this.frameworkSourcePanel = frameworkSourcePanel;
    }


    @NotNull
    @Override
    public Set<? extends LibraryKind> getSuitableLibraryKinds() {
        return Sets.newHashSet(KOTLIN_JAVA_RUNTIME_KIND);
    }

    @Nullable
    @Override
    public NewLibraryConfiguration createNewLibrary(@NotNull JComponent parentComponent, @Nullable VirtualFile contextDirectory) {
        if (frameworkSourcePanel.isConfigureFromBundled()) {
            final FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);

            descriptor.setTitle("Select Folder");
            descriptor.setDescription("Select folder where bundled Kotlin java runtime library should be copied");

            final VirtualFile[] files = FileChooser.chooseFiles(descriptor, parentComponent, null, contextDirectory);
            if (files.length == 0) {
                return null;
            }

            assert files.length == 1: "Only one folder is expected";

            final VirtualFile directory = files[0];
            assert directory.isDirectory();

            return new NewLibraryConfiguration(KotlinRuntimeLibraryUtil.LIBRARY_NAME, getDownloadableLibraryType(), new LibraryVersionProperties()) {
                @Override
                public void addRoots(@NotNull LibraryEditor editor) {
                    editor.addRoot(directory.getUrl() + "/" + KotlinRuntimeLibraryUtil.KOTLIN_RUNTIME_JAR, OrderRootType.CLASSES);
                }
            };
        }
        else {
            throw new InvalidOpenTypeException("Isn't supported yet");
        }
    }
}