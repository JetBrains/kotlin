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

package org.jetbrains.jet.plugin.actions;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetMainDetector;
import org.jetbrains.k2js.facade.K2JSTranslator;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.plugin.actions.JavaToKotlinActionUtil.allVirtualFiles;

//TODO: clean up

/**
 * @author Pavel Talanov
 */
public final class TranslateToJsAction extends AnAction {

    private static void notifyFailure(@NotNull Throwable exception) {
        Notifications.Bus.notify(new Notification("JsTranslator", "Translation failed.",
                                                  "Exception: " + exception.getMessage(),
                                                  NotificationType.ERROR));
    }

    private static void notifySuccess(@NotNull String outputPath) {
        Notifications.Bus.notify(new Notification("JsTranslator", "Translation successful.",
                                                  "Generated file: " + outputPath,
                                                  NotificationType.INFORMATION));
    }

    public void actionPerformed(final AnActionEvent event) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    performAction(event);
                } catch (Throwable e) {
                    e.printStackTrace();
                    notifyFailure(e);
                }
            }
        };
        ApplicationManager.getApplication().runWriteAction(task);
    }

    private static void performAction(@NotNull AnActionEvent event) throws Exception {
        final Project project = PlatformDataKeys.PROJECT.getData(event.getDataContext());
        assert project != null;
        Set<VirtualFile> allVirtualFiles = getAllProjectVirtualFiles(project);
        List<JetFile> kotlinFiles = getJetFiles(allVirtualFiles, project);
        String outputPath = getOutputPath(JetMainDetector.getFileWithMain(kotlinFiles));
        K2JSTranslator.translateWithCallToMainAndSaveToFile(kotlinFiles,
                                                            outputPath,
                                                            project);
        notifySuccess(outputPath);
    }

    @NotNull
    private static Set<VirtualFile> getAllProjectVirtualFiles(@NotNull Project project) {
        Module[] modules = ModuleManager.getInstance(project).getModules();
        Set<VirtualFile> allVirtualFiles = Sets.newHashSet();
        for (Module module : modules) {
            VirtualFile[] roots = ModuleRootManager.getInstance(module).getSourceRoots();
            allVirtualFiles.addAll(allVirtualFiles(roots));
        }
        return allVirtualFiles;
    }

    @NotNull
    private static List<JetFile> getJetFiles(@NotNull Collection<VirtualFile> virtualFiles,
                                             @NotNull Project project) {
        List<JetFile> kotlinFiles = Lists.newArrayList();

        PsiManager psiManager = PsiManager.getInstance(project);
        for (VirtualFile virtualFile : virtualFiles) {
            PsiFile psiFile = psiManager.findFile(virtualFile);
            if (psiFile instanceof JetFile) {
                kotlinFiles.add((JetFile) psiFile);
            }
        }
        return kotlinFiles;
    }


    @NotNull
    private static String getOutputPath(@Nullable PsiFile psiFile) {
        if (psiFile == null) {
            throw new AssertionError("Main was not detected.");
        }
        VirtualFile virtualFile = psiFile.getVirtualFile();
        assert virtualFile != null : "Internal error: Psi file should correspond to actual virtual file";
        String originalFilePath = virtualFile.getPath();

        //TODO: make platform independent
        String pathToDir = originalFilePath.substring(0, originalFilePath.lastIndexOf("/") + 1);
        String generatedFileName = ((JetFile) psiFile).getNamespaceHeader().getName() + ".js";
        return pathToDir + generatedFileName;
    }

    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        e.getPresentation().setEnabled(editor != null);
    }
}
