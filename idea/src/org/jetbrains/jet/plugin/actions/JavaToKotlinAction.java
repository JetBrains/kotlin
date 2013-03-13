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

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.jet.j2k.Converter;

import java.util.List;

import static org.jetbrains.jet.plugin.actions.JavaToKotlinActionUtil.*;

public class JavaToKotlinAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        VirtualFile[] virtualFiles = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
        assert virtualFiles != null;
        final Project project = PlatformDataKeys.PROJECT.getData(e.getDataContext());
        assert project != null;
        final Converter converter = new Converter(project);
        int result = Messages.showYesNoCancelDialog(project,
                                                    "Would you like to backup Java files?",
                                                    "Backup",
                                                    Messages.getQuestionIcon());
        final boolean finalRemoveIt = needToRemoveFiles(result);
        final List<PsiFile> allJavaFiles = getAllJavaFiles(virtualFiles, project);

        converter.clearClassIdentifiers();
        for (PsiFile f : allJavaFiles) {
            if (f.getFileType() instanceof JavaFileType) {
                setClassIdentifiers(converter, f);
            }
        }

        final List<PsiFile> allJavaFilesNear = getAllJavaFiles(virtualFiles, project);
        CommandProcessor.getInstance().executeCommand(
                project,
                new Runnable() {
                    @Override
                    public void run() {
                        List<VirtualFile> newFiles = convertFiles(converter, allJavaFilesNear);
                        if (finalRemoveIt) {
                            deleteFiles(allJavaFilesNear);
                        }
                        else {
                            renameFiles(allJavaFiles);
                        }
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

    private static boolean needToRemoveFiles(int result) {
        boolean removeIt = false;
        switch (result) {
            case 0:
                removeIt = false;
                break;
            case 1:
                removeIt = true;
                break;
        }
        return removeIt;
    }

    @Override
    public void update(AnActionEvent e) {
        boolean enabled = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY) != null;
        e.getPresentation().setVisible(enabled);
        e.getPresentation().setEnabled(enabled);
    }
}
