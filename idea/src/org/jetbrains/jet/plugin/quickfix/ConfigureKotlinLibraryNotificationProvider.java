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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileTextField;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetFileType;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class ConfigureKotlinLibraryNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> {
    private static final Key<EditorNotificationPanel> KEY = Key.create("configure.kotlin.library");
    private final Project myProject;

    public ConfigureKotlinLibraryNotificationProvider(Project project) {
        myProject = project;
    }

    @Override
    public Key<EditorNotificationPanel> getKey() {
        return KEY;
    }

    @Override
    @Nullable
    public EditorNotificationPanel createNotificationPanel(VirtualFile file, FileEditor fileEditor) {
        try {
            if (file.getFileType() != JetFileType.INSTANCE) return null;

            if (CompilerManager.getInstance(myProject).isExcludedFromCompilation(file)) return null;

            final Module module = ModuleUtilCore.findModuleForFile(file, myProject);
            if (module == null) return null;

            if (!KotlinRuntimeLibraryUtil.isModuleAlreadyConfigured(module)) {
                return createNotificationPanel(module);
            }
        }
        catch (ProcessCanceledException e) {
            // Ignore
        }
        catch (IndexNotReadyException e) {
            // Ignore
        }

        return null;
    }


    private EditorNotificationPanel createNotificationPanel(final Module module) {
        final EditorNotificationPanel answer = new EditorNotificationPanel();

        answer.setText("Kotlin is not configured for module '" + module.getName() + "'");
        answer.createActionLabel("Set up module '" + module.getName() + "' as JVM Kotlin module", new Runnable() {
            @Override
            public void run() {
                setUpJavaModule(module);
            }
        });

        answer.createActionLabel("Set up module '" + module.getName() + "' as JavaScript Kotlin module", new Runnable() {
            @Override
            public void run() {
                setUpJSModule(module);
            }
        });

        return answer;
    }

    private void setUpJavaModule(Module module) {
        Library library = KotlinRuntimeLibraryUtil.findOrCreateRuntimeLibrary(myProject, new UiFindRuntimeLibraryHandler());
        if (library == null) return;

        KotlinRuntimeLibraryUtil.setUpKotlinRuntimeLibrary(module, library, new Runnable() {
            @Override
            public void run() {
                updateNotifications();
            }
        });
    }

    private void setUpJSModule(@NotNull Module module) {
        JsModuleSetUp.doSetUpModule(module, new Runnable() {
            @Override
            public void run() {
                updateNotifications();
            }
        });
    }

    private void updateNotifications() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                EditorNotifications.getInstance(myProject).updateAllNotifications();
            }
        });
    }

    private static class ChoosePathDialog extends DialogWrapper {
        private final Project myProject;
        private TextFieldWithBrowseButton myPathField;

        protected ChoosePathDialog(Project project) {
            super(project);
            myProject = project;

            setTitle("Local Kotlin Runtime Path");
            init();
        }

        @Override
        protected JComponent createCenterPanel() {
            FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
            FileTextField field = FileChooserFactory.getInstance().createFileTextField(descriptor, myDisposable);
            field.getField().setColumns(25);
            myPathField = new TextFieldWithBrowseButton(field.getField());
            myPathField.addBrowseFolderListener("Choose Destination Folder", "Choose folder for file", myProject, descriptor);

            VirtualFile baseDir = myProject.getBaseDir();
            if (baseDir != null) {
                myPathField.setText(baseDir.getPath().replace('/', File.separatorChar) + File.separatorChar + "lib");
            }

            return myPathField;
        }

        public String getPath() {
            return myPathField.getText();
        }
    }

    private class UiFindRuntimeLibraryHandler extends KotlinRuntimeLibraryUtil.FindRuntimeLibraryHandler {
        @Override
        public void runtimePathDoesNotExist(@NotNull File path) {
            Messages.showErrorDialog(myProject,
                                     "kotlin-runtime.jar is not found at " + path + ". Make sure plugin is properly installed.",
                                     "No Runtime Found");
        }

        @Override
        public File getRuntimeJarPath() {
            ChoosePathDialog dlg = new ChoosePathDialog(myProject);
            dlg.show();
            if (!dlg.isOK()) return null;
            String path = dlg.getPath();
            return new File(path, "kotlin-runtime.jar");
        }

        @Override
        public void ioExceptionOnCopyingJar(@NotNull IOException e) {
            Messages.showErrorDialog(myProject, "Error copying jar: " + e.getLocalizedMessage(), "Error Copying File");
        }
    }
}
