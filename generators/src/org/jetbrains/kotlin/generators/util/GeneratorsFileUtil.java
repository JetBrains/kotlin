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

package org.jetbrains.kotlin.generators.util;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;

import java.io.File;
import java.io.IOException;

public class GeneratorsFileUtil {
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void writeFileIfContentChanged(File file, String newText) throws IOException {
        writeFileIfContentChanged(file, newText, true);
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void writeFileIfContentChanged(File file, String newText, boolean logNotChanged) throws IOException {
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            if (parentFile.mkdirs()) {
                System.out.println("Directory created: " + parentFile.getAbsolutePath());
            }
            else {
                throw new IllegalStateException("Cannot create directory: " + parentFile);
            }
        }

        if (checkFileIgnoringLineSeparators(file, newText)) {
            if (logNotChanged) {
                System.out.println("Not changed: " + file.getAbsolutePath());
            }
            return;
        }

        boolean useTempFile = !SystemInfo.isWindows;

        File tempFile = useTempFile ? new File(file.getName() + ".tmp") : file;

        FileUtil.writeToFile(tempFile, newText);
        System.out.println("File written: " + tempFile.getAbsolutePath());
        if (useTempFile) {
            if (!tempFile.renameTo(file)) {
                throw new RuntimeException("failed to rename " + tempFile + " to " + file);
            }
            System.out.println("Renamed " + tempFile + " to " + file);
        }
        System.out.println();
    }

    private static boolean checkFileIgnoringLineSeparators(File file, String content) {
        String currentContent;
        try {
            currentContent = FileUtil.loadFile(file, true);
        }
        catch (Throwable ignored) {
            return false;
        }

        return StringUtil.convertLineSeparators(content).equals(currentContent);
    }
}
