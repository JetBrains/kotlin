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

package org.jetbrains.jet.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.j2k.ConverterSettings;
import org.jetbrains.jet.j2k.FilesConversionScope;
import org.jetbrains.jet.j2k.IdeaReferenceSearcher;
import org.jetbrains.jet.j2k.JavaToKotlinConverter;

import java.util.List;

import static org.jetbrains.jet.plugin.actions.JavaToKotlinActionUtil.*;

public class JavaToKotlinAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        VirtualFile[] virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        assert virtualFiles != null;
        final Project project = CommonDataKeys.PROJECT.getData(e.getDataContext());
        assert project != null;
        final List<PsiJavaFile> selectedJavaFiles = getAllJavaFiles(virtualFiles, project);
        if (selectedJavaFiles.isEmpty()) {
            return;
        }

        final JavaToKotlinConverter converter = new JavaToKotlinConverter(project,
                                                             ConverterSettings.defaultSettings,
                                                             new FilesConversionScope(selectedJavaFiles),
                                                             IdeaReferenceSearcher.INSTANCE$);
        CommandProcessor.getInstance().executeCommand(
                project,
                new Runnable() {
                    @Override
                    public void run() {
                        List<VirtualFile> newFiles = convertFiles(converter, selectedJavaFiles);
                        deleteFiles(selectedJavaFiles);
                        reformatFiles(newFiles, project);
                        for (VirtualFile vf : newFiles) {
                            FileEditorManager.getInstance(project).openFile(vf, true);
                        }
                    }
                },
                "Convert files from Java to Kotlin",
                "group_id"
        );
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        boolean enabled = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) != null;
        e.getPresentation().setVisible(enabled);
        e.getPresentation().setEnabled(enabled);
    }
}
