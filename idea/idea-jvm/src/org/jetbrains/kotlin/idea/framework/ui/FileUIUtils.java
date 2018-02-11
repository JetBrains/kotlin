/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.framework.ui;

import com.google.common.collect.ImmutableMap;
import com.intellij.ide.util.projectWizard.ProjectWizardUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class FileUIUtils {
    private FileUIUtils() {
    }

    @Nullable
    public static File copyWithOverwriteDialog(
            @NotNull String messagesTitle,
            @NotNull String destinationFolder,
            @NotNull File file
    ) {
        Map<File, File> copiedFiles = copyWithOverwriteDialog(messagesTitle, ImmutableMap.of(file, destinationFolder));
        if (copiedFiles == null) {
            return null;
        }

        File copy = copiedFiles.get(file);
        assert copy != null;

        return copy;
    }

    @Nullable
    public static Map<File, File> copyWithOverwriteDialog(
            @NotNull String messagesTitle,
            @NotNull Map<File, String> filesWithDestinations
    ) {
        Set<String> fileNames = new HashSet<String>();
        Map<File, File> targetFiles = new LinkedHashMap<File, File>(filesWithDestinations.size());
        for (Map.Entry<File, String> sourceToDestination : filesWithDestinations.entrySet()) {
            File file = sourceToDestination.getKey();
            String destinationPath = sourceToDestination.getValue();

            String fileName = file.getName();

            if (!fileNames.add(fileName)) {
                throw new IllegalArgumentException("There are several files with the same name: " + fileName);
            }

            targetFiles.put(file, new File(destinationPath, fileName));
        }

        Collection<Map.Entry<File, File>> existentFiles =
                CollectionsKt.filter(targetFiles.entrySet(), sourceToTarget -> sourceToTarget.getValue().exists());

        if (!existentFiles.isEmpty()) {
            String message;

            if (existentFiles.size() == 1) {
                File conflictingFile = existentFiles.iterator().next().getValue();
                message = String.format("File \"%s\" already exists in %s.\nDo you want to overwrite it?", conflictingFile.getName(),
                                        conflictingFile.getParentFile().getAbsolutePath());
            }
            else {
                Collection<File> conflictFiles = CollectionsKt.map(existentFiles, Map.Entry::getValue);
                message = String.format("Files already exist:\n%s\nDo you want to overwrite them?", StringUtil.join(conflictFiles, "\n"));
            }

            int replaceIfExist = Messages.showYesNoDialog(
                    null,
                    message,
                    messagesTitle + ". Replace File",
                    "Overwrite",
                    "Cancel",
                    Messages.getWarningIcon());

            if (replaceIfExist != JOptionPane.YES_OPTION) {
                return null;
            }
        }

        for (Map.Entry<File, File> sourceToTarget : targetFiles.entrySet()) {
            try {
                String destinationPath = sourceToTarget.getValue().getParentFile().getAbsolutePath();
                if (!ProjectWizardUtil.createDirectoryIfNotExists("Destination folder", destinationPath, false)) {
                    Messages.showErrorDialog(String.format("Error during folder creating '%s'", destinationPath), messagesTitle + ". Error");
                    return null;
                }

                FileUtil.copy(sourceToTarget.getKey(), sourceToTarget.getValue());
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(sourceToTarget.getValue());
            }
            catch (IOException e) {
                Messages.showErrorDialog("Error with copy file " + sourceToTarget.getKey().getName(), messagesTitle + ". Error");
                return null;
            }
        }

        return targetFiles;
    }

    @NotNull
    public static String createRelativePath(@Nullable Project project, @Nullable VirtualFile contextDirectory, String relativePath) {
        String path = null;
        if (contextDirectory != null) {
            path = PathUtil.getLocalPath(contextDirectory);

        }
        else if (project != null) {
            path = PathUtil.getLocalPath(project.getBaseDir());
        }

        if (path != null) {
            path = new File(path, relativePath).getAbsolutePath();
        }
        else {
            path = "";
        }
        return path;
    }
}
