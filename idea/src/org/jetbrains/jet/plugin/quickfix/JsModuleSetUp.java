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

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiModificationTrackerImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.project.K2JSModuleComponent;
import org.jetbrains.jet.plugin.util.PluginPathUtil;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.jetbrains.jet.plugin.k2jsrun.K2JSRunnerUtils.copyFileToDir;

/**
 * @author Pavel Talanov
 */
public final class JsModuleSetUp {

    private JsModuleSetUp() {
    }

    public static void doSetUpModule(@Nullable Module module, @NotNull Runnable continuation) {
        if (module == null) {
            notifyFailure("Internal error: Module not found.");
            return;
        }

        File rootDir = getRootDir(module);
        if (!rootDir.isDirectory()) {
            notifyFailure("Internal error: Broken content root.");
            return;
        }

        if (!copyJsLibFiles(module, rootDir)) return;

        setUpK2JSModuleComponent(module);
        setUpLibraryAsSourceLibrary(module, rootDir);

        restartHighlightingInTheWholeProject(module);

        refreshRootDir(module, continuation);
    }

    private static void setUpLibraryAsSourceLibrary(final Module module, final @NotNull File rootDir) {
        final File libJarFile = new File(rootDir, "lib/" + PathUtil.JS_LIB_JAR_NAME);
        final VirtualFile libFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(libJarFile);

        if (libFile != null) {
            ApplicationManager.getApplication().runWriteAction(new Runnable() {
                @Override
                public void run() {
                    ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
                    LibraryTable table = model.getModuleLibraryTable();
                    Library jsLib = table.getLibraryByName(PathUtil.JS_LIB_JAR_NAME);
                    if (jsLib == null) {
                        VirtualFile libRoot = JarFileSystem.getInstance().getJarRootForLocalFile(libFile);
                        LibrariesContainerFactory.createContainer(model).createLibrary(PathUtil.JS_LIB_JAR_NAME,
                                LibrariesContainer.LibraryLevel.MODULE, new VirtualFile[0], new VirtualFile[] { libRoot });
                        model.commit();
                    }
                }
            });
        }
    }

    private static void setUpK2JSModuleComponent(@NotNull Module module) {
        K2JSModuleComponent jsModuleComponent = K2JSModuleComponent.getInstance(module);
        jsModuleComponent.setJavaScriptModule(true);
        jsModuleComponent.setPathToJavaScriptLibrary("/lib/" + PathUtil.JS_LIB_JAR_NAME);
    }

    private static void restartHighlightingInTheWholeProject(@NotNull Module module) {
        ((PsiModificationTrackerImpl) PsiManager.getInstance(module.getProject()).getModificationTracker()).incCounter();
        DaemonCodeAnalyzer.getInstance(module.getProject()).restart();
    }

    private static boolean copyJsLibFiles(@NotNull Module module, @NotNull File rootDir) {
        File jsLibJarPath = PluginPathUtil.getJsLibJarPath(module);
        File jsLibJsPath = PluginPathUtil.getJsLibJsPath(module);
        if ((jsLibJarPath == null) || (jsLibJsPath == null)) {
            notifyFailure("JavaScript library not found. Make sure plugin is installed properly.");
            return false;
        }

        return doCopyJsLibFiles(Arrays.asList(jsLibJarPath, jsLibJsPath), rootDir);
    }

    private static void refreshRootDir(@NotNull Module project, @NotNull Runnable continuation) {
        getContentRoot(project).refresh(true, true, continuation);
    }

    private static boolean doCopyJsLibFiles(@NotNull List<File> files, @NotNull File rootDir) {
        try {
            File lib = new File(rootDir, "lib");
            for (File file : files) {
                copyFileToDir(file, lib);
            }
        }
        catch (IOException e) {
            notifyFailure("Failed to copy file: " + e.getMessage());
            return false;
        }
        return true;
    }

    @NotNull
    private static File getRootDir(@NotNull Module module) {
        VirtualFile contentRoot = getContentRoot(module);
        return new File(contentRoot.getPath());
    }

    @NotNull
    private static VirtualFile getContentRoot(@NotNull Module module) {
        return ModuleRootManager.getInstance(module).getContentRoots()[0];
    }

    public static void notifyFailure(@NotNull String message) {
        Notifications.Bus.notify(new Notification("Set Up Kotlin to JavaScript Module", "Failure",
                                                  message,
                                                  NotificationType.ERROR));
    }
}
