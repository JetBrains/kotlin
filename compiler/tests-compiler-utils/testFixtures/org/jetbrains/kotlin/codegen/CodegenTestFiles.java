/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiErrorElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.AnalyzingUtils;
import org.jetbrains.kotlin.test.util.KtTestUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CodegenTestFiles {
    @NotNull
    private final List<KtFile> psiFiles;

    private CodegenTestFiles(@NotNull List<KtFile> psiFiles) {
        this.psiFiles = psiFiles;
    }

    @NotNull
    public KtFile getPsiFile() {
        assert psiFiles.size() == 1;
        return psiFiles.get(0);
    }

    @NotNull
    public List<KtFile> getPsiFiles() {
        return psiFiles;
    }

    @NotNull
    public static CodegenTestFiles create(@NotNull String fileName, @NotNull String contentWithDiagnosticMarkup, @NotNull Project project) {
        // `rangesToDiagnosticNames` parameter is not-null only for diagnostic tests, it's using for lazy diagnostics
        String content = CheckerTestUtil.INSTANCE.parseDiagnosedRanges(contentWithDiagnosticMarkup, new ArrayList<>(), null);
        KtFile file = KtTestUtil.createFile(fileName, content, project);
        List<PsiErrorElement> ranges = AnalyzingUtils.getSyntaxErrorRanges(file);
        assert ranges.isEmpty() : "Syntax errors found in " + file + ": " + ranges;

        return new CodegenTestFiles(Collections.singletonList(file));
    }
}
