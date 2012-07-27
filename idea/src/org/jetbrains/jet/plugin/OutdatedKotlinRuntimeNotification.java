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

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleComponent;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
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
import org.jetbrains.jet.plugin.util.PluginPathUtil;
import org.jetbrains.jet.utils.PathUtil;

import javax.swing.event.HyperlinkEvent;
import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * @author Evgeny Gerashchenko
 * @since 5/22/12
 */
public class OutdatedKotlinRuntimeNotification implements ModuleComponent {
    private static final String UNKNOWN_VERSION = "UNKNOWN";
    private static final String SUPPRESSED_PROPERTY_NAME_PATTERN = "oudtdated.runtime.suppressed.plugin.version[%s]";

    private final Module myModule;
    private final String mySuppressedPropertyName;

    public OutdatedKotlinRuntimeNotification(Module module) {
        myModule = module;
        mySuppressedPropertyName = String.format(SUPPRESSED_PROPERTY_NAME_PATTERN, module.getName());
    }

    @Override
    public void projectOpened() {
        if (ApplicationManager.getApplication().isInternal()) return;
        String runtimeVersion = getRuntimeVersion(getKotlinRuntimeJar());
        if (runtimeVersion == null) return; // runtime is not present in project
        final String sdkVersion = getRuntimeVersion(getRuntimeFromSdk());
        if (sdkVersion == null || "snapshot".equals(sdkVersion)) return; // plugin is run from sources or SDK is not configured for the module, can't compare versions

        // user already clicked suppress
        if (sdkVersion.equals(PropertiesComponent.getInstance(myModule.getProject()).getValue(mySuppressedPropertyName))) return;

        boolean isRuntimeOutdated = "snapshot".equals(runtimeVersion)
                                    || UNKNOWN_VERSION.equals(runtimeVersion)
                                    || runtimeVersion.startsWith("internal-") != sdkVersion.startsWith("internal-")
                                    || VersionComparatorUtil.compare(sdkVersion, runtimeVersion) > 0;

        if (!isRuntimeOutdated) return;

        String message = String.format("<p>Your version of Kotlin runtime library in module \"%s\" is %s, while Kotlin SDK version in this module is %s." +
                                       " Runtime library should be updated to avoid compatibility problems.</p>" +
                                       "<p><a href=\"update\">Update Runtime</a> <a href=\"ignore\">Ignore</a></p>",
                                       myModule.getName(), UNKNOWN_VERSION.equals(runtimeVersion) ? "older than 0.1.2296" : runtimeVersion, sdkVersion);
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
                        PropertiesComponent.getInstance(myModule.getProject()).setValue(mySuppressedPropertyName, sdkVersion);
                    }
                    else {
                        throw new AssertionError();
                    }
                    notification.expire();
                }
            }
        }), myModule.getProject());
    }

    private void updateRuntime() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                File runtimePath = PluginPathUtil.getRuntimePath(myModule);
                if (runtimePath == null) {
                    Messages.showErrorDialog(myModule.getProject(),
                                             "\"kotlin-runtime.jar\" is not found. Make sure Kotlin SDK is configured for module \"" + myModule.getName() + "\".",
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
    private static String getRuntimeVersion(@Nullable VirtualFile kotlinRuntimeJar) {
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
        LibraryTable table = ModuleRootManager.getInstance(myModule).getModifiableModel().getModuleLibraryTable();
        Library kotlinRuntime = table.getLibraryByName(ConfigureKotlinLibraryNotificationProvider.LIBRARY_NAME);
        if (kotlinRuntime != null) {
            for (VirtualFile root : kotlinRuntime.getFiles(OrderRootType.CLASSES)) {
                if (root.getName().equals(PathUtil.KOTLIN_RUNTIME_JAR)) {
                    return root;
                }
            }
        }
        return null;
    }

    @Nullable
    private VirtualFile getRuntimeFromSdk() {
        File runtimePath = PluginPathUtil.getRuntimePath(myModule);
        return runtimePath == null ? null : PathUtil.jarFileOrDirectoryToVirtualFile(runtimePath);
    }

    @Override
    public void moduleAdded() {}

    @Override
    public void projectClosed() {}

    @Override
    public void initComponent() {}

    @Override
    public void disposeComponent() {}

    @NotNull
    @Override
    public String getComponentName() {
        return getClass().getName();
    }
}
