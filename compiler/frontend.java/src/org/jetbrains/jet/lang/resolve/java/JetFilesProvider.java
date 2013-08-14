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

package org.jetbrains.jet.lang.resolve.java;

import com.google.common.base.Predicate;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public abstract class JetFilesProvider {
    public static JetFilesProvider getInstance(Project project) {
        return ServiceManager.getService(project, JetFilesProvider.class);
    }

    public final Function<JetFile, List<JetFile>> allNamespaceFiles() {
        return new Function<JetFile, List<JetFile>>() {
            @Override
            public List<JetFile> fun(JetFile file) {
                return new SameJetFilePredicate(file).filter(sampleToAllFilesInModule().fun(file));
            }
        };
    }

    public abstract Function<JetFile, Collection<JetFile>> sampleToAllFilesInModule();
    public abstract Collection<JetFile> allInScope(@NotNull GlobalSearchScope scope);
    public abstract boolean isFileInScope(@NotNull JetFile file, @NotNull GlobalSearchScope scope);

    public static class SameJetFilePredicate implements Predicate<PsiFile> {
        private final FqName name;

        public SameJetFilePredicate(JetFile file) {
            this.name = JetPsiUtil.getFQName(file);
        }

        @Override
        public boolean apply(PsiFile psiFile) {
            return JetPsiUtil.getFQName((JetFile) psiFile).equals(name);
        }

        public List<JetFile> filter(Collection<JetFile> allFiles) {
            LinkedList<JetFile> files = new LinkedList<JetFile>();
            for (JetFile aFile : allFiles) {
                if(apply(aFile))
                    files.add(aFile);
            }
            return files;
        }
    }
}
