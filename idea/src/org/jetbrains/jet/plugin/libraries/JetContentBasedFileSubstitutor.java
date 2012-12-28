/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.libraries;

import com.intellij.lang.Language;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.ContentBasedClassFileProcessor;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.highlighter.JetHighlighter;

public class JetContentBasedFileSubstitutor implements ContentBasedClassFileProcessor {

    @Override
    public boolean isApplicable(@Nullable Project project, @NotNull final VirtualFile file) {
        if (project == null) {
            return false;
        }

        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart(new Runnable() {
                @Override
                public void run() {
                    FileDocumentManager docManager = FileDocumentManager.getInstance();
                    docManager.getDocument(file); // force getting document because it can be collected
                    docManager.reloadFiles(file);
                }
            });
            return false;
        }

        return JetDecompiledData.isKotlinFile(project, file);
    }

    @NotNull
    @Override
    public String obtainFileText(Project project, VirtualFile file) {
        if (JetDecompiledData.isKotlinFile(project, file)) {
            ClsFileImpl clsFile = JetDecompiledData.getClsFile(project, file);
            assert clsFile != null;
            return JetDecompiledData.getDecompiledData(clsFile).getJetFile().getText();
        }
        return "";
    }

    @Override
    public Language obtainLanguageForFile(VirtualFile file) {
        return null;
    }

    @NotNull
    @Override
    public SyntaxHighlighter createHighlighter(Project project, VirtualFile vFile) {
        return new JetHighlighter();
    }
}

