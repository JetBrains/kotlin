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

/*
 * @author max
 */
package org.jetbrains.jet.plugin.project;

import com.google.common.collect.Sets;
import com.intellij.openapi.compiler.ex.CompilerPathsEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Function;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.JetFilesProvider;
import org.jetbrains.jet.plugin.JetFileType;

import java.util.*;

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
                    ModuleRootManager.getInstance(module).getFileIndex().iterateContent(new ContentIterator() {
                        @Override
                        public boolean processFile(VirtualFile file) {
                            if (file.isDirectory()) return true;

                            final FileType fileType = FileTypeManager.getInstance().getFileTypeByFile(file);
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
    public List<JetFile> allInScope(final GlobalSearchScope scope) {
        final List<JetFile> answer = new ArrayList<JetFile>();

        final FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        List<VirtualFile> contentRoots = Arrays.asList(ProjectRootManager.getInstance(project).getContentRoots());
        final PsiManager manager = PsiManager.getInstance(project);

        CompilerPathsEx.visitFiles(contentRoots, new CompilerPathsEx.FileVisitor() {
            @Override
            protected void acceptFile(VirtualFile file, String fileRoot, String filePath) {
                final FileType fileType = fileTypeManager.getFileTypeByFile(file);
                if (fileType != JetFileType.INSTANCE) return;

                if (scope.accept(file)) {
                    final PsiFile psiFile = manager.findFile(file);
                    if (psiFile instanceof JetFile) {
                        answer.add((JetFile) psiFile);
                    }
                }
            }
        });

        return answer;
    }
}
