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

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class FileUIUtils {
    private FileUIUtils() {
    }

    @NotNull
    public static File copyWithOverwriteDialog(@NotNull String destinationFolder, @NotNull File file) throws IOException {
        File folder = new File(destinationFolder);
        File targetFile = new File(folder, file.getName());

        // TODO: create folder if it is not present yet
        assert folder.exists();

        if (!targetFile.exists()) {
            FileUtil.copy(file, targetFile);
        }
        else {
            int replaceIfExist = Messages.showYesNoDialog(
                    String.format("File \"%s\" is already exist in %s.\nDo you want to rewrite it?", targetFile.getName(),
                                  folder.getAbsolutePath()),
                    "Replace File", Messages.getWarningIcon());

            if (replaceIfExist == JOptionPane.YES_OPTION) {
                FileUtil.copy(file, targetFile);
            }
        }

        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(targetFile);

        return targetFile;
    }

    public static File copyWithOverwriteDialog(@NotNull VirtualFile directory, @NotNull File path) throws IOException {
        String localPath = PathUtil.getLocalPath(directory);
        assert localPath != null;

        return copyWithOverwriteDialog(localPath, path);
    }

    @Nullable
    public static VirtualFile selectCopyToDirectory(
            @NotNull String description,
            @Nullable JComponent parentComponent,
            @Nullable VirtualFile contextDirectory) {
        final FileChooserDescriptor descriptor = new FileChooserDescriptor(false, true, false, false, false, false);

        descriptor.setTitle("Select Folder");
        descriptor.setDescription(description);

        final VirtualFile[] files = FileChooser.chooseFiles(descriptor, parentComponent, null, contextDirectory);
        if (files.length == 0) {
            return null;
        }

        assert files.length == 1: "Only one folder is expected";

        final VirtualFile directory = files[0];
        assert directory.isDirectory();

        return directory;
    }
}
