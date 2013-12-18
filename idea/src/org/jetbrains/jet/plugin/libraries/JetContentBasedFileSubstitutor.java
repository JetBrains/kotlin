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

package org.jetbrains.jet.plugin.libraries;

import com.google.common.collect.Maps;
import com.intellij.AppTopics;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerAdapter;
import com.intellij.openapi.fileTypes.ContentBasedClassFileProcessor;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.impl.PsiDocumentManagerBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.highlighter.JetHighlighter;

import java.util.concurrent.ConcurrentMap;

public final class JetContentBasedFileSubstitutor implements ContentBasedClassFileProcessor {
    private static final JetContentBasedFileSubstitutor instance = new JetContentBasedFileSubstitutor();

    private static final ConcurrentMap<VirtualFile, JetFile> deferredDocumentBinding = Maps.newConcurrentMap();

    public static JetContentBasedFileSubstitutor getInstance() {
        return instance;
    }

    private JetContentBasedFileSubstitutor() {
        ApplicationManager.getApplication().getMessageBus().connect().subscribe(AppTopics.FILE_DOCUMENT_SYNC, new FileDocumentManagerAdapter() {
            @Override
            public void fileContentLoaded(@NotNull VirtualFile loadedFile, @NotNull Document document) {
                processDeferredBindings(loadedFile, document);
            }

            @Override
            public void fileContentReloaded(VirtualFile loadedFile, @NotNull Document document) {
                processDeferredBindings(loadedFile, document);
            }

            private void processDeferredBindings(VirtualFile loadedFile, @NotNull Document document) {
                JetFile file = deferredDocumentBinding.remove(loadedFile);
                if (file != null) {
                    PsiDocumentManagerBase.cachePsi(document, file);
                }
            }
        });
    }

    @Override
    public boolean isApplicable(@Nullable Project project, @NotNull final VirtualFile file) {
        if (project == null) {
            return false;
        }

        if (DumbService.isDumb(project)) {
            DumbService.getInstance(project).runWhenSmart(new Runnable() {
                @Override
                public void run() {
                    if (DecompiledUtils.isKotlinCompiledFile(file)) {
                        FileDocumentManager docManager = FileDocumentManager.getInstance();
                        docManager.getDocument(file); // force getting document because it can be collected
                        docManager.reloadFiles(file);
                    }
                }
            });
            return false;
        }

        return DecompiledUtils.isKotlinCompiledFile(file);
    }

    @NotNull
    @Override
    public String obtainFileText(Project project, VirtualFile file) {
        if (file != null && DecompiledUtils.isKotlinCompiledFile(file)) {
            JetDecompiledData data = JetDecompiledData.getDecompiledData(file, project);
            deferredDocumentBinding.put(file, data.getFile());

            return data.getFileText();
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

