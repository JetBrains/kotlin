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

package org.jetbrains.jet.plugin.versions;

import com.intellij.ProjectTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootAdapter;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.framework.KotlinFrameworkDetector;
import org.jetbrains.jet.plugin.framework.JSFrameworkSupportProvider;
import org.jetbrains.jet.plugin.framework.JavaFrameworkSupportProvider;
import org.jetbrains.jet.plugin.framework.ui.AddSupportForSingleFrameworkDialogFixed;

public class KotlinLibrariesNotificationProvider extends EditorNotifications.Provider<EditorNotificationPanel> {
    private static final Key<EditorNotificationPanel> KEY = Key.create("configure.kotlin.library");
    private final Project myProject;
    private final Runnable updateNotifications = new Runnable() {
        @Override
        public void run() {
            updateNotifications();
        }
    };

    public KotlinLibrariesNotificationProvider(Project project) {
        myProject = project;
        MessageBusConnection connection = myProject.getMessageBus().connect();
        connection.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootAdapter() {
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                updateNotifications();
            }
        });
        connection.subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
            @Override
            public void enteredDumbMode() {}

            @Override
            public void exitDumbMode() {
                updateNotifications();
            }
        });
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

            Module module = ModuleUtilCore.findModuleForFile(file, myProject);
            if (module == null) return null;

            if (!isModuleAlreadyConfigured(module)) {
                return createFrameworkConfigurationNotificationPanel(module);
            }

            return UnsupportedAbiVersionNotificationPanelProvider.checkAndCreate(myProject);
        }
        catch (ProcessCanceledException e) {
            // Ignore
        }
        catch (IndexNotReadyException e) {
            DumbService.getInstance(myProject).runWhenSmart(updateNotifications);
        }

        return null;
    }

    public static boolean isModuleAlreadyConfigured(Module module) {
        return isMavenModule(module) || KotlinFrameworkDetector.isJsKotlinModule(module) || KotlinFrameworkDetector.isJavaKotlinModule(module);
    }

    private static boolean isMavenModule(@NotNull Module module) {
        // This constant could be acquired from MavenProjectsManager, but we don't want to depend on the Maven plugin...
        // See MavenProjectsManager.isMavenizedModule()
        return "true".equals(module.getOptionValue("org.jetbrains.idea.maven.project.MavenProjectsManager.isMavenModule"));
    }


    private static EditorNotificationPanel createFrameworkConfigurationNotificationPanel(final Module module) {
        EditorNotificationPanel answer = new EditorNotificationPanel();

        answer.setText("Kotlin is not configured for module '" + module.getName() + "'");
        answer.createActionLabel("Set up module '" + module.getName() + "' as JVM Kotlin module", new Runnable() {
            @Override
            public void run() {
                DialogWrapper dialog = AddSupportForSingleFrameworkDialogFixed.createDialog(
                        module, new JavaFrameworkSupportProvider());
                if (dialog != null) {
                    dialog.show();
                }
            }
        });

        answer.createActionLabel("Set up module '" + module.getName() + "' as JavaScript Kotlin module", new Runnable() {
            @Override
            public void run() {
                DialogWrapper dialog = AddSupportForSingleFrameworkDialogFixed.createDialog(
                        module, new JSFrameworkSupportProvider());
                if (dialog != null) {
                    dialog.show();
                }
            }
        });

        return answer;
    }

    private void updateNotifications() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                EditorNotifications.getInstance(myProject).updateAllNotifications();
            }
        });
    }
}
