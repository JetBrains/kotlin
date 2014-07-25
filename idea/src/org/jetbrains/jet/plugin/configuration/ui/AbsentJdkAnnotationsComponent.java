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
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationsConfiguration;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootAdapter;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.*;
import com.intellij.util.Alarm;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.configuration.AbsentSdkAnnotationsNotificationManager;
import org.jetbrains.jet.plugin.configuration.ConfigureKotlinInProjectUtils;
import org.jetbrains.jet.plugin.configuration.ui.notifications.NotificationsPackage;
import org.jetbrains.jet.plugin.project.ProjectStructureUtil;
import org.jetbrains.jet.plugin.versions.KotlinRuntimeLibraryUtil;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class AbsentJdkAnnotationsComponent extends AbstractProjectComponent {
    public static final String EXTERNAL_ANNOTATIONS_GROUP_ID = "Kotlin External annotations";
    private volatile Alarm notificationAlarm;

    protected AbsentJdkAnnotationsComponent(Project project) {
        super(project);

        NotificationsConfiguration.getNotificationsConfiguration().
                register(EXTERNAL_ANNOTATIONS_GROUP_ID, NotificationDisplayType.STICKY_BALLOON, true);
    }

    @Override
    public void projectOpened() {
        super.projectOpened();
        MessageBusConnection connection = myProject.getMessageBus().connect();

        notificationAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, myProject);

        connection.subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
            @Override
            public void enteredDumbMode() {
            }

            @Override
            public void exitDumbMode() {
                scheduleNotificationCheck();
            }
        });

        connection.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootAdapter() {
            @Override
            public void rootsChanged(ModuleRootEvent event) {
                scheduleNotificationCheck();
            }
        });

        connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener.Adapter() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
                for (VFileEvent event : events) {
                    if (event instanceof VFileCreateEvent || event instanceof VFileDeleteEvent ||
                            event instanceof VFileMoveEvent || event instanceof VFileCopyEvent) {
                        scheduleNotificationCheck();
                        break;
                    }
                }
            }
        });
    }

    private void scheduleNotificationCheck() {
        if (notificationAlarm.isDisposed()) {
            return;
        }

        notificationAlarm.cancelAllRequests();

        notificationAlarm.addRequest(new Runnable() {
            @Override
            public void run() {
                DumbService.getInstance(myProject).smartInvokeLater(new Runnable() {
                    @Override
                    public void run() {
                        Collection<Sdk> sdks = collectSdksWithoutAnnotations();
                        if (!sdks.isEmpty()) {
                            AbsentSdkAnnotationsNotificationManager.INSTANCE$.notify(myProject, sdks);
                        }
                    }
                });
            }
        }, TimeUnit.SECONDS.toMillis(1));
    }

    @NotNull
    private Collection<Sdk> collectSdksWithoutAnnotations() {
        Set<Sdk> sdks = Sets.newHashSet();
        for (Module module : ConfigureKotlinInProjectUtils.getModulesWithKotlinFiles(myProject)) {
            if (ProjectStructureUtil.isJavaKotlinModule(module)) {
                Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
                if (sdk != null && !areAnnotationsCorrect(sdk)) {
                    sdks.add(sdk);
                }
            }
        }
        return sdks;
    }

    private static boolean areAnnotationsCorrect(@NotNull Sdk sdk) {
        return NotificationsPackage.isAndroidSdk(sdk)
               ? KotlinRuntimeLibraryUtil.androidSdkAnnotationsArePresent(sdk) && !KotlinRuntimeLibraryUtil.jdkAnnotationsArePresent(sdk)
               : KotlinRuntimeLibraryUtil.jdkAnnotationsArePresent(sdk);
    }
}