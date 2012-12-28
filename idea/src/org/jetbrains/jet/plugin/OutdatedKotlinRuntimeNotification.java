package org.jetbrains.jet.plugin;/*
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

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.AbstractProjectComponent;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.impl.elements.ManifestFileUtil;
import com.intellij.util.text.VersionComparatorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.quickfix.ConfigureKotlinLibraryNotificationProvider;
import org.jetbrains.jet.utils.PathUtil;

import javax.swing.event.HyperlinkEvent;
import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

public class OutdatedKotlinRuntimeNotification extends AbstractProjectComponent {
    private static final String UNKNOWN_VERSION = "UNKNOWN";
    private static final String SUPPRESSED_PROPERTY_NAME = "oudtdated.runtime.suppressed.plugin.version";

    public OutdatedKotlinRuntimeNotification(final Project project) {
        super(project);
    }

    @Override
    public void projectOpened() {
        if (ApplicationManager.getApplication().isInternal()) return;
        String runtimeVersion = getRuntimeVersion();
        final String pluginVersion = getPluginVersion();
        if (runtimeVersion == null) return; // runtime is not present in project
        if ("@snapshot@".equals(pluginVersion)) return; // plugin is run from sources, can't compare versions

        // user already clicked suppress
        if (pluginVersion.equals(PropertiesComponent.getInstance(myProject).getValue(SUPPRESSED_PROPERTY_NAME))) return;

        boolean isRuntimeOutdated = "snapshot".equals(runtimeVersion)
                                    || UNKNOWN_VERSION.equals(runtimeVersion)
                                    || runtimeVersion.startsWith("internal-") != pluginVersion.startsWith("internal-")
                                    || VersionComparatorUtil.compare(pluginVersion, runtimeVersion) > 0;

        if (!isRuntimeOutdated) return;

        String message = String.format("<p>Your version of Kotlin runtime library is %s, while plugin version is %s." +
                                       " Runtime library should be updated to avoid compatibility problems.</p>" +
                                       "<p><a href=\"update\">Update Runtime</a> <a href=\"ignore\">Ignore</a></p>",
                                       UNKNOWN_VERSION.equals(runtimeVersion) ? "older than 0.1.2296" : runtimeVersion, pluginVersion);
        Notifications.Bus.notify(new Notification("Outdated Kotlin Runtime", "Outdated Kotlin Runtime",
                                                  message,
                                                  NotificationType.WARNING, new NotificationListener() {
            @Override
            public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if ("update".equals(event.getDescription())) {
                        updateRuntime();
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

    private void updateRuntime() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                File runtimePath = PathUtil.getKotlinPathsForIdeaPlugin().getRuntimePath();
                if (!runtimePath.exists()) {
                    Messages.showErrorDialog(myProject, "kotlin-runtime.jar is not found. Make sure plugin is properly installed.",
                                             "No Runtime Found");
                    return;
                }
                VirtualFile runtimeJar = getKotlinRuntimeJar();
                assert runtimeJar != null;
                VirtualFile jarFile = JarFileSystem.getInstance().getVirtualFileForJar(runtimeJar);
                if (jarFile != null) {
                    runtimeJar = jarFile;
                }

                try {
                    FileUtil.copy(runtimePath, new File(runtimeJar.getPath()));
                }
                catch (IOException e) {
                    throw new AssertionError(e);
                }
                runtimeJar.refresh(true, true);
            }
        });
    }

    @Nullable
    private String getRuntimeVersion() {
        VirtualFile kotlinRuntimeJar = getKotlinRuntimeJar();
        if (kotlinRuntimeJar == null) return null;
        VirtualFile manifestFile = kotlinRuntimeJar.findFileByRelativePath(JarFile.MANIFEST_NAME);
        if (manifestFile != null) {
            Attributes attributes = ManifestFileUtil.readManifest(manifestFile).getMainAttributes();
            if (attributes.containsKey(Attributes.Name.IMPLEMENTATION_VERSION)) {
                return attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            }
        }
        return UNKNOWN_VERSION;
    }

    @Nullable
    private VirtualFile getKotlinRuntimeJar() {
        LibraryTable table = ProjectLibraryTable.getInstance(myProject);
        Library kotlinRuntime = table.getLibraryByName(ConfigureKotlinLibraryNotificationProvider.LIBRARY_NAME);
        if (kotlinRuntime != null) {
            for (VirtualFile root : kotlinRuntime.getFiles(OrderRootType.CLASSES)) {
                if (root.getName().equals(ConfigureKotlinLibraryNotificationProvider.KOTLIN_RUNTIME_JAR)) {
                    return root;
                }
            }
        }
        return null;
    }

    @NotNull
    private static String getPluginVersion() {
        IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId("org.jetbrains.kotlin"));
        assert plugin != null : "How can it be? Kotlin plugin is available, but its component is running. Complete nonsense.";
        return plugin.getVersion();
    }
}
