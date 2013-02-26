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

package org.jetbrains.jet.plugin.framework.ui;

import com.intellij.ide.util.projectWizard.ProjectWizardUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class FileUIUtils {
    private FileUIUtils() {
    }

    @Nullable
    public static File copyWithOverwriteDialog(
            @NotNull Component parent,
            @NotNull String destinationFolder,
            @NotNull File file,
            @NotNull String messagesTitle) {
        if (!ProjectWizardUtil.createDirectoryIfNotExists("Destination folder", destinationFolder, false)) {
            Messages.showErrorDialog(String.format("Error during folder creating '%s'", destinationFolder), messagesTitle + ". Error");
            return null;
        }

        File folder = new File(destinationFolder);
        File targetFile = new File(folder, file.getName());

        assert folder.exists();

        try {
            if (!targetFile.exists()) {
                FileUtil.copy(file, targetFile);
            }
            else {
                int replaceIfExist = Messages.showYesNoDialog(
                        parent,
                        String.format("File \"%s\" is already exist in %s.\nDo you want to rewrite it?", targetFile.getName(), folder.getAbsolutePath()),
                        messagesTitle + ". Replace File",
                        Messages.getWarningIcon());

                if (replaceIfExist == JOptionPane.YES_OPTION) {
                    FileUtil.copy(file, targetFile);
                }
            }

            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetFile);

            return targetFile;
        }
        catch (IOException e) {
            Messages.showErrorDialog("Error during file copy", messagesTitle + ". Error");
            return null;
        }
    }

    @NotNull
    public static String getDefaultLibraryFolder(@Nullable Project project, @Nullable VirtualFile contextDirectory) {
        String path = null;
        if (contextDirectory != null) {
            path = PathUtil.getLocalPath(contextDirectory);

        }
        else if (project != null) {
            path = PathUtil.getLocalPath(project.getBaseDir());
        }

        if (path != null) {
            path = new File(path, "lib").getAbsolutePath();
        }
        else {
            path = "";
        }
        return path;
    }
}
