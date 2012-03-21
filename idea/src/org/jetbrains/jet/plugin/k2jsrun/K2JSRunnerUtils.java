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

package org.jetbrains.jet.plugin.k2jsrun;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.facade.K2JSTranslator;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.plugin.actions.JavaToKotlinActionUtil.allVirtualFiles;

//TODO: clean up

/**
 * @author Pavel Talanov
 */
public final class K2JSRunnerUtils {

    public static void notifyFailure(@NotNull Throwable exception) {
        Notifications.Bus.notify(new Notification("JsTranslator", "Translation failed.",
                                                  "Exception: " + exception.getMessage(),
                                                  NotificationType.ERROR));
    }

    private static void notifySuccess(@NotNull String outputPath) {
        Notifications.Bus.notify(new Notification("JsTranslator", "Translation successful.",
                                                  "Generated file: " + outputPath,
                                                  NotificationType.INFORMATION));
    }

    public static void translateAndSave(@NotNull Project project, @NotNull String outputDirPath) throws Exception {
        Set<VirtualFile> allVirtualFiles = getAllProjectVirtualFiles(project);
        List<JetFile> kotlinFiles = getJetFiles(allVirtualFiles, project);
        String outputFilePath = constructPathToGeneratedFile(project, outputDirPath);
        K2JSTranslator.translateWithCallToMainAndSaveToFile(kotlinFiles,
                                                            outputFilePath,
                                                            project);
        notifySuccess(outputDirPath);
    }

    private static String constructPathToGeneratedFile(Project project, String outputDirPath) {
        return outputDirPath + "/" + project.getName() + ".js";
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
                kotlinFiles.add((JetFile)psiFile);
            }
        }
        return kotlinFiles;
    }
}
