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

package org.jetbrains.jet.plugin.compiler;

import com.google.common.collect.Sets;
import com.intellij.openapi.compiler.ex.CompilerPathsEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.JetFileUtil;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.plugin.JetFileType;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public final class WholeProjectAnalyzerFacade {

    /** Forbid creating */
    private WholeProjectAnalyzerFacade() {}

    /**
     * Will collect all root-namespaces in all kotlin files in the project.
     */
    public static final Function<JetFile, Collection<JetFile>> WHOLE_PROJECT_DECLARATION_PROVIDER = new Function<JetFile, Collection<JetFile>>() {

        @Override
        public Collection<JetFile> fun(final JetFile rootFile) {
            final Project project = rootFile.getProject();
            final Set<JetFile> files = Sets.newLinkedHashSet();
            final ProjectRootManager rootManager = ProjectRootManager.getInstance(project);

            if (rootManager != null /* && !ApplicationManager.getApplication().isUnitTestMode() */) {
                List<VirtualFile> contentRoots = Arrays.asList(rootManager.getContentRoots());

                CompilerPathsEx.visitFiles(contentRoots, new CompilerPathsEx.FileVisitor() {
                    @Override
                    protected void acceptFile(VirtualFile file, String fileRoot, String filePath) {
                        final FileType fileType = FileTypeManager.getInstance().getFileTypeByFile(file);
                        if (fileType != JetFileType.INSTANCE) return;
                        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                        if (psiFile instanceof JetFile) {
                            if (rootFile.getOriginalFile() != psiFile) {
                                files.add((JetFile) psiFile);
                            }
                        }
                    }
                });
            }

            files.add(rootFile);
            return files;
        }
    };

    @NotNull
    public static BindingContext analyzeProjectWithCacheOnAFile(@NotNull JetFile file) {
        return AnalyzerFacadeForJVM.analyzeFileWithCache(file, WHOLE_PROJECT_DECLARATION_PROVIDER);
    }

    @NotNull
    public static BindingContext analyzeProjectWithCache(@NotNull Project project, @NotNull GlobalSearchScope scope) {
        return AnalyzerFacadeForJVM.analyzeProjectWithCache(project, JetFileUtil.collectJetFiles(project, scope));
    }
}
