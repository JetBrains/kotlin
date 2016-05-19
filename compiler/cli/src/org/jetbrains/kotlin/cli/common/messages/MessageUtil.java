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

package org.jetbrains.kotlin.cli.common.messages;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.impl.jar.CoreJarVirtualFile;
import com.intellij.openapi.vfs.local.CoreLocalVirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils;

import static com.intellij.openapi.util.io.FileUtil.toSystemDependentName;

public class MessageUtil {
    private MessageUtil() {}

    @NotNull
    public static CompilerMessageLocation psiElementToMessageLocation(@Nullable PsiElement element) {
        if (element == null) return CompilerMessageLocation.NO_LOCATION;
        PsiFile file = element.getContainingFile();
        return psiFileToMessageLocation(file, "<no path>", DiagnosticUtils.getLineAndColumnInPsiFile(file, element.getTextRange()));
    }

    @NotNull
    public static CompilerMessageLocation psiFileToMessageLocation(
            @NotNull PsiFile file,
            @Nullable String defaultValue,
            @NotNull DiagnosticUtils.LineAndColumn lineAndColumn
    ) {
        String path;
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) {
            path = defaultValue;
        }
        else {
            path = virtualFile.getPath();
            // Convert path to platform-dependent format when virtualFile is local file.
            if (virtualFile instanceof CoreLocalVirtualFile || virtualFile instanceof CoreJarVirtualFile) {
                path = toSystemDependentName(path);
            }
        }
        return CompilerMessageLocation.create(path, lineAndColumn.getLine(), lineAndColumn.getColumn(), lineAndColumn.getLineContent());
    }
}
