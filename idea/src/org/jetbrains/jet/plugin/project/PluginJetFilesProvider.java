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

package org.jetbrains.jet.plugin.project;

import com.google.common.collect.Sets;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ModuleFileIndex;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.JetPluginUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class PluginJetFilesProvider  {

    public static Collection<JetFile> allFilesInProject(@NotNull final JetFile rootFile) {
        final Project project = rootFile.getProject();
        final Set<JetFile> files = Sets.newLinkedHashSet();

        Module rootModule = ModuleUtil.findModuleForPsiElement(rootFile);
        if (rootModule != null) {
            Set<Module> allModules = new HashSet<Module>();
            ModuleUtil.getDependencies(rootModule, allModules);

            for (Module module : allModules) {
                final ModuleFileIndex index = ModuleRootManager.getInstance(module).getFileIndex();
                index.iterateContent(new ContentIterator() {
                    @Override
                    public boolean processFile(VirtualFile file) {
                        if (file.isDirectory()) return true;
                        if (!index.isInSourceContent(file) && !index.isInTestSourceContent(file)) return true;
                        if (JetPluginUtil.isKtFileInGradleProjectInWrongFolder(file, project)) return true;

                        FileType fileType = FileTypeManager.getInstance().getFileTypeByFile(file);
                        if (fileType != JetFileType.INSTANCE) return true;
                        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                        if (psiFile instanceof JetFile) {
                            if (rootFile.getOriginalFile() != psiFile) {
                                files.add((JetFile) psiFile);
                            }
                        }
                        return true;
                    }
                });
            }
        }

        files.add(rootFile);
        return files;
    }

    private PluginJetFilesProvider() {
    }
}
