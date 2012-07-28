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

import com.intellij.ProjectTopics;
import com.intellij.ide.util.frameworkSupport.AddFrameworkSupportDialog;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetPluginUtil;

/**
 * @author Maxim.Manuylov
 *         Date: 19.05.12
 */
public class KotlinSdkNotConfiguredNotificationProvider implements EditorNotifications.Provider<EditorNotificationPanel> {
    @NotNull private static final Key<EditorNotificationPanel> KEY = Key.create("configure.kotlin.sdk");

    @NotNull private final Project myProject;

    public KotlinSdkNotConfiguredNotificationProvider(@NotNull Project project, @NotNull final EditorNotifications notifications) {
        myProject = project;
        project.getMessageBus().connect(project).subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
            @Override
            public void beforeRootsChange(ModuleRootEvent event) {}

            @Override
            public void rootsChanged(ModuleRootEvent event) {
                notifications.updateAllNotifications();
            }
        });
    }

    @NotNull
    @Override
    public Key<EditorNotificationPanel> getKey() {
        return KEY;
    }

    @Nullable
    @Override
    public EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file) {
        try {
            Module module = JetPluginUtil.getModuleForKotlinFile(file, myProject);
            if (module == null) return null;

            if (!KotlinSdkUtil.isSDKConfiguredFor(module)) {
                return createNotificationPanel(module);
            }
        }
        catch (ProcessCanceledException ignore) {}
        catch (IndexNotReadyException ignore) {}

        return null;
    }

    @NotNull
    private static EditorNotificationPanel createNotificationPanel(@NotNull final Module module) {
        EditorNotificationPanel panel = new EditorNotificationPanel();
        panel.setText("Kotlin SDK is not configured for module \"" + module.getName() + "\"");
        panel.createActionLabel("Configure Kotlin SDK", new Runnable() {
            @Override
            public void run() {
                AddFrameworkSupportDialog dialog = AddFrameworkSupportDialog.createDialog(module);
                if (dialog != null) {
                    dialog.show();
                }
            }
        });
        return panel;
    }
}
