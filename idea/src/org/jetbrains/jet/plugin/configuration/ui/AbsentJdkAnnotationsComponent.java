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

package org.jetbrains.jet.plugin.configuration.ui;

import com.google.common.collect.Sets;
import com.intellij.ProjectTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootAdapter;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.impl.BulkVirtualFileListenerAdapter;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.configuration.AbsentSdkAnnotationsNotificationManager;
import org.jetbrains.jet.plugin.configuration.ConfigureKotlinInProjectUtils;
import org.jetbrains.jet.plugin.configuration.ui.notifications.NotificationsPackage;
import org.jetbrains.jet.plugin.framework.KotlinFrameworkDetector;
import org.jetbrains.jet.plugin.versions.KotlinRuntimeLibraryUtil;

import java.util.Collection;
import java.util.Set;

public class AbsentJdkAnnotationsComponent extends AbstractProjectComponent {

    protected AbsentJdkAnnotationsComponent(Project project) {
        super(project);
    }

    @Override
    public void projectOpened() {
        super.projectOpened();
        MessageBusConnection connection = myProject.getMessageBus().connect();
        connection.subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
            @Override
            public void enteredDumbMode() {
            }

            @Override
            public void exitDumbMode() {
                showNotificationIfNeeded();
            }
        });
        connection.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootAdapter() {
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                showNotificationIfNeeded();
            }
        });

        connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkVirtualFileListenerAdapter(new VirtualFileAdapter() {
            @Override
            public void fileDeleted(VirtualFileEvent event) {
                showNotificationIfNeeded();
            }

            @Override
            public void fileCreated(VirtualFileEvent event) {
                showNotificationIfNeeded();
            }

            @Override
            public void fileMoved(VirtualFileMoveEvent event) {
                showNotificationIfNeeded();
            }

            @Override
            public void fileCopied(VirtualFileCopyEvent event) {
                showNotificationIfNeeded();
            }
        }));
    }

    public void showNotificationIfNeeded() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                Collection<Sdk> sdks = collectSdksWithoutAnnotations();
                if (sdks.isEmpty()) return;
                AbsentSdkAnnotationsNotificationManager.instance$.notify(myProject, sdks);
            }
        });
    }

    @NotNull
    private Collection<Sdk> collectSdksWithoutAnnotations() {
        Set<Sdk> sdks = Sets.newHashSet();
        for (Module module : ConfigureKotlinInProjectUtils.getModulesWithKotlinFiles(myProject)) {
            if (KotlinFrameworkDetector.isJavaKotlinModule(module)) {
                Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
                if (sdk != null && !isAnnotationsArePresent(sdk)) {
                    sdks.add(sdk);
                }
            }
        }
        return sdks;
    }

    private static boolean isAnnotationsArePresent(@NotNull Sdk sdk) {
        if (!KotlinRuntimeLibraryUtil.jdkAnnotationsArePresent(sdk)) {
            return false;
        }

        boolean isAndroidSdk = NotificationsPackage.isAndroidSdk(sdk);
        return !(isAndroidSdk && !KotlinRuntimeLibraryUtil.androidSdkAnnotationsArePresent(sdk));
    }
}