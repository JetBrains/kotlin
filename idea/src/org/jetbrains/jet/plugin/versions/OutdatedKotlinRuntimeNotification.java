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

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.text.VersionComparatorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetPluginUtil;

import javax.swing.event.HyperlinkEvent;

public class OutdatedKotlinRuntimeNotification extends AbstractProjectComponent {
    private static final String SUPPRESSED_PROPERTY_NAME = "oudtdated.runtime.suppressed.plugin.version";

    public OutdatedKotlinRuntimeNotification(Project project) {
        super(project);
    }

    @Override
    public void projectOpened() {
        if (ApplicationManager.getApplication().isInternal()) return;
        StartupManager.getInstance(myProject).registerPostStartupActivity(new Runnable() {
            @Override
            public void run() {
                String runtimeVersion = KotlinRuntimeLibraryUtil.getRuntimeVersion(myProject);
                final String pluginVersion = JetPluginUtil.getPluginVersion();
                if (runtimeVersion == null) return; // runtime is not present in project
                if ("@snapshot@".equals(pluginVersion)) return; // plugin is run from sources, can't compare versions

                // user already clicked suppress
                if (pluginVersion.equals(PropertiesComponent.getInstance(myProject).getValue(SUPPRESSED_PROPERTY_NAME))) return;

                boolean isRuntimeOutdated = "snapshot".equals(runtimeVersion)
                                            || KotlinRuntimeLibraryUtil.UNKNOWN_VERSION.equals(runtimeVersion)
                                            || runtimeVersion.startsWith("internal-") != pluginVersion.startsWith("internal-")
                                            || VersionComparatorUtil.compare(pluginVersion, runtimeVersion) > 0;

                if (!isRuntimeOutdated) return;

                String message = String.format("<p>Your version of Kotlin runtime library is %s, while plugin version is %s." +
                                               " Runtime library should be updated to avoid compatibility problems.</p>" +
                                               "<p><a href=\"update\">Update Runtime</a> <a href=\"ignore\">Ignore</a></p>",
                                               KotlinRuntimeLibraryUtil.UNKNOWN_VERSION.equals(runtimeVersion)
                                               ? "older than 0.1.2296"
                                               : runtimeVersion, pluginVersion);
                Notifications.Bus.notify(new Notification("Outdated Kotlin Runtime", "Outdated Kotlin Runtime",
                                                          message,
                                                          NotificationType.WARNING, new NotificationListener() {
                    @Override
                    public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                            if ("update".equals(event.getDescription())) {
                                KotlinRuntimeLibraryUtil.updateRuntime(myProject, showRuntimeJarNotFoundDialog(myProject));
                            }
                            else if ("ignore".equals(event.getDescription())) {
                                PropertiesComponent.getInstance(myProject).setValue(SUPPRESSED_PROPERTY_NAME, pluginVersion);
                            }
                            else {
                                throw new AssertionError();
                            }
                            notification.expire();
                        }
                    }
                }), myProject);
            }
        });
    }

    @NotNull
    public static Runnable showRuntimeJarNotFoundDialog(@NotNull final Project project) {
        return new Runnable() {
            @Override
            public void run() {
                Messages.showErrorDialog(project,
                                         "kotlin-runtime.jar is not found. Make sure plugin is properly installed.",
                                         "No Runtime Found");
            }
        };
    }
}
