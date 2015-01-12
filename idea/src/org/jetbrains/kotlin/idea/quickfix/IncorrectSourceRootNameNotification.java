/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix;

import com.intellij.ide.actions.OpenFileAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.JetFileType;
import org.jetbrains.kotlin.idea.JetPluginUtil;

import java.io.IOException;

public class IncorrectSourceRootNameNotification extends EditorNotifications.Provider<EditorNotificationPanel> {
    private static final Key<EditorNotificationPanel> KEY = Key.create("move.kotlin.file.under.kotlin.source.root");

    private final Project project;

    public IncorrectSourceRootNameNotification(Project project) {
        this.project = project;
    }

    @Override
    @Nullable
    public EditorNotificationPanel createNotificationPanel(@NotNull final VirtualFile file, @NotNull FileEditor fileEditor) {
        if (file.getFileType() != JetFileType.INSTANCE) return null;
        if (CompilerManager.getInstance(project).isExcludedFromCompilation(file)) return null;

        if (!JetPluginUtil.isKtFileInGradleProjectInWrongFolder(file, project)) {
            return null;
        }

        Module module = ModuleUtilCore.findModuleForFile(file, project);
        if (module == null) return null;

        EditorNotificationPanel panel = new EditorNotificationPanel();
        panel.setText("Kotlin file in Gradle Project should be under source root with name 'kotlin'");

        ModuleRootManager moduleManager = ModuleRootManager.getInstance(module);
        boolean isInTestSourceRoot = moduleManager.getFileIndex().isInTestSourceContent(file);

        for (final VirtualFile root : moduleManager.getSourceRoots(isInTestSourceRoot)) {
            if (root.getName().equals("kotlin") && moduleManager.getFileIndex().isInTestSourceContent(root) == isInTestSourceRoot) {
                VirtualFile sourceRootForFile = ProjectRootManager.getInstance(project).getFileIndex().getSourceRootForFile(file);
                assert sourceRootForFile != null : "Notification Panel should appear only for files under source root " +
                                                   file.getCanonicalPath();
                final String relativePath = VfsUtilCore.getRelativePath(file, sourceRootForFile, '/');
                if (relativePath != null) {
                    VirtualFile inKotlinDir = VfsUtil.findRelativeFile(root, relativePath.split("/"));
                    if (inKotlinDir == null) {
                        panel.createActionLabel("Move file", new Runnable() {
                            @Override
                            public void run() {
                                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            VirtualFile newFile = VfsUtil.
                                                    copyFileRelative(IncorrectSourceRootNameNotification.class, file, root, relativePath);
                                            file.delete(IncorrectSourceRootNameNotification.class);
                                            OpenFileAction.openFile(newFile, project);
                                        }
                                        catch (IOException ignored) {
                                        }
                                    }
                                });
                            }
                        });
                        break;
                    }
                }
            }
        }
        return panel;
    }

    @Override
    public Key<EditorNotificationPanel> getKey() {
        return KEY;
    }
}
