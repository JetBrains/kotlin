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
import com.intellij.util.Function;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.JetFilesProvider;
import org.jetbrains.jet.plugin.JetFileType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class PluginJetFilesProvider extends JetFilesProvider {
    private final Project project;

    public PluginJetFilesProvider(Project project) {
        this.project = project;
    }

    public static final Function<JetFile, Collection<JetFile>> WHOLE_PROJECT_DECLARATION_PROVIDER = new Function<JetFile, Collection<JetFile>>() {

        @Override
        public Collection<JetFile> fun(final JetFile rootFile) {
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
    };

    @Override
    public Function<JetFile, Collection<JetFile>> sampleToAllFilesInModule() {
        return WHOLE_PROJECT_DECLARATION_PROVIDER;
    }

    @Override
    public Collection<JetFile> allInScope(GlobalSearchScope scope) {
        final PsiManager manager = PsiManager.getInstance(project);

        Collection<JetFile> jetFiles = Collections2.transform(FileTypeIndex.getFiles(JetFileType.INSTANCE, scope),
               new com.google.common.base.Function<VirtualFile, JetFile>() {
                   @Override
                   public JetFile apply(@Nullable VirtualFile file) {
                       if (file == null || !ProjectFileIndex.SERVICE.getInstance(project).isInSourceContent(file)) {
                           return null;
                       }

                       PsiFile psiFile = manager.findFile(file);
                       if (!(psiFile instanceof JetFile)) {
                           return null;
                       }

                       return ((JetFile) psiFile);
                   }
               });

        return Sets.newHashSet(Collections2.filter(jetFiles, Predicates.<JetFile>notNull()));
    }
}
