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

import java.util.List;

import static org.jetbrains.jet.j2k.Converter.clearClassIdentifiers;
import static org.jetbrains.jet.plugin.actions.JavaToKotlinActionUtil.*;

/**
 * @author ignatov
 */
public class JavaToKotlinAction extends AnAction {
    @Override
    public void actionPerformed(final AnActionEvent e) {
        VirtualFile[] virtualFiles = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
        final Project project = PlatformDataKeys.PROJECT.getData(e.getDataContext());
        assert virtualFiles != null;
        assert project != null;
        int result = Messages.showYesNoCancelDialog(project,
                                                    "Would you like to backup Java files?",
                                                    "Backup",
                                                    Messages.getQuestionIcon());
        final boolean finalRemoveIt = needToRemoveFiles(result);
        final List<PsiFile> allJavaFiles = getAllJavaFiles(virtualFiles, project);

        clearClassIdentifiers();
        for (PsiFile f : allJavaFiles) {
            if (f.getFileType() instanceof JavaFileType) {
                setClassIdentifiers(f);
            }
        }

        final List<PsiFile> allJavaFilesNear = getAllJavaFiles(virtualFiles, project);
        CommandProcessor.getInstance().executeCommand(
                project,
                new Runnable() {
                    @Override
                    public void run() {
                        final List<VirtualFile> newFiles = performFiles(allJavaFilesNear);
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
        final boolean enabled = e.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY) != null;
        e.getPresentation().setVisible(enabled);
        e.getPresentation().setEnabled(enabled);
    }
}
