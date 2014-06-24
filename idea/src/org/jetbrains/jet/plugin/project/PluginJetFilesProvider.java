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

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ModuleFileIndex;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.*;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.JetFilesProvider;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.JetPluginUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PluginJetFilesProvider extends JetFilesProvider {
    private final Project project;

    private final ParameterizedCachedValue<Collection<JetFile>, GlobalSearchScope> cache;

    public PluginJetFilesProvider(Project project) {
        this.project = project;
        this.cache = CachedValuesManager.getManager(project).createParameterizedCachedValue(
                new ParameterizedCachedValueProvider<Collection<JetFile>, GlobalSearchScope>() {
                    @Nullable
                    @Override
                    public CachedValueProvider.Result<Collection<JetFile>> compute(GlobalSearchScope param) {
                        return new CachedValueProvider.Result<Collection<JetFile>>(
                                computeAllInScope(param),
                                PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT
                        );
                    }
                },
                false
        );
    }

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

    private boolean isKotlinSourceVirtualFile(@NotNull VirtualFile virtualFile) {
        return ProjectFileIndex.SERVICE.getInstance(project).isInSourceContent(virtualFile) &&
               !JetPluginUtil.isKtFileInGradleProjectInWrongFolder(virtualFile, project);
    }

    @NotNull
    @Override
    public Collection<JetFile> allInScope(@NotNull GlobalSearchScope scope) {
        return cache.getValue(scope);
    }

    private Collection<JetFile> computeAllInScope(GlobalSearchScope scope) {
        final PsiManager manager = PsiManager.getInstance(project);

        Collection<JetFile> jetFiles = ContainerUtil.map(
                FileTypeIndex.getFiles(JetFileType.INSTANCE, scope),
                new Function<VirtualFile, JetFile>() {
                    @Override
                    public JetFile fun(@Nullable VirtualFile file) {
                        if (file == null || !isKotlinSourceVirtualFile(file)) {
                            return null;
                        }

                        PsiFile psiFile = manager.findFile(file);
                        if (!(psiFile instanceof JetFile)) {
                            return null;
                        }

                        return ((JetFile) psiFile);
                    }
                });

        return Collections.unmodifiableCollection(
                Sets.newHashSet(Collections2.filter(jetFiles, Predicates.<JetFile>notNull()))
        );
    }

    @Override
    public boolean isFileInScope(@NotNull JetFile file, @NotNull GlobalSearchScope scope) {
        VirtualFile virtualFile = file.getVirtualFile();
        return virtualFile != null && scope.contains(virtualFile) && isKotlinSourceVirtualFile(virtualFile);
    }
}
