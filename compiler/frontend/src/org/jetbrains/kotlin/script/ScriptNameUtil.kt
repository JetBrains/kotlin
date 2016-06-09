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

package org.jetbrains.kotlin.script;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtScript;

public class ScriptNameUtil {
    private ScriptNameUtil() {
    }

    @NotNull
    public static Name fileNameWithExtensionStripped(@NotNull KtScript script, @NotNull String extension) {
        KtFile file = script.getContainingKtFile();
        return Name.identifier(generateNameByFileName(file.getName(), extension));
    }

    @NotNull
    public static String generateNameByFileName(@NotNull String fileName, @NotNull String extension) {
        int index = fileName.lastIndexOf('/');
        if (index != -1) {
            fileName = fileName.substring(index + 1);
        }
        if (fileName.endsWith(extension)) {
            fileName = fileName.substring(0, fileName.length() - extension.length());
        }
        else {
            index = fileName.indexOf('.');
            if (index != -1) {
                fileName = fileName.substring(0, index);
            }
        }
        fileName = Character.toUpperCase(fileName.charAt(0)) + (fileName.length() == 0 ? "" : fileName.substring(1));
        fileName = fileName.replace('.', '_');
        return fileName;
    }
}
