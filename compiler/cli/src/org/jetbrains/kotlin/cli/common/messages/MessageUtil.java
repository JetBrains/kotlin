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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils;
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils;

import static com.intellij.openapi.util.io.FileUtil.toSystemDependentName;

public class MessageUtil {
    private MessageUtil() {}

    @Nullable
    public static CompilerMessageSourceLocation psiElementToMessageLocation(@Nullable PsiElement element) {
        if (element == null) return null;
        PsiFile file = element.getContainingFile();
        return psiFileToMessageLocation(file, "<no path>", DiagnosticUtils.getLineAndColumnRangeInPsiFile(file, element.getTextRange()));
    }

    @Nullable
    public static CompilerMessageSourceLocation psiFileToMessageLocation(
            @NotNull PsiFile file,
            @Nullable String defaultValue,
            @NotNull PsiDiagnosticUtils.LineAndColumnRange range
    ) {
        VirtualFile virtualFile = file.getVirtualFile();
        String path = virtualFile != null ? virtualFileToPath(virtualFile) : defaultValue;
        PsiDiagnosticUtils.LineAndColumn start = range.getStart();
        PsiDiagnosticUtils.LineAndColumn end = range.getEnd();
        return CompilerMessageLocationWithRange.create(path, start.getLine(), start.getColumn(), end.getLine(), end.getColumn(), start.getLineContent());
    }

    @NotNull
    public static String virtualFileToPath(@NotNull VirtualFile virtualFile) {
        return toSystemDependentName(virtualFile.getPath());
    }
}
