/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.lang.psi.JetFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Stepan Koltsov
 */
public class CodegenTestFile {

    @NotNull
    private final JetFile psiFile;
    @NotNull
    private final String expectedValue;

    private CodegenTestFile(@NotNull JetFile psiFile, @NotNull String expectedValue) {
        this.psiFile = psiFile;
        this.expectedValue = expectedValue;
    }

    @NotNull
    public JetFile getPsiFile() {
        return psiFile;
    }

    @NotNull
    public String getExpectedValue() {
        return expectedValue;
    }

    @NotNull
    public static CodegenTestFile create(@NotNull String name, @NotNull String content, @NotNull Project project) {
        JetFile file = (JetFile) JetTestUtils.createFile(name, content, project);

        Matcher matcher = Pattern.compile("// expected: (.*)").matcher(content);
        String expectedValue = matcher.find() ? matcher.group(1) : "OK";

        return new CodegenTestFile(file, expectedValue);
    }
}
