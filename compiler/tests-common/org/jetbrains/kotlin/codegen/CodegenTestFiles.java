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

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiErrorElement;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.checkers.CheckerTestUtil;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.AnalyzingUtils;
import org.jetbrains.kotlin.script.KotlinScriptDefinitionProvider;
import org.jetbrains.kotlin.script.StandardScriptDefinition;
import org.jetbrains.kotlin.test.KotlinTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodegenTestFiles {

    @NotNull
    private final List<KtFile> psiFiles;
    @NotNull
    private final List<Pair<String, String>> expectedValues;
    @NotNull
    private final List<Object> scriptParameterValues;

    private CodegenTestFiles(
            @NotNull List<KtFile> psiFiles,
            @NotNull List<Pair<String, String>> expectedValues,
            @NotNull List<Object> scriptParameterValues
    ) {
        this.psiFiles = psiFiles;
        this.expectedValues = expectedValues;
        this.scriptParameterValues = scriptParameterValues;
    }

    @NotNull
    public KtFile getPsiFile() {
        assert psiFiles.size() == 1;
        return psiFiles.get(0);
    }

    @NotNull
    public List<Pair<String, String>> getExpectedValues() {
        return expectedValues;
    }

    @NotNull
    public List<Object> getScriptParameterValues() {
        return scriptParameterValues;
    }

    @NotNull
    public List<KtFile> getPsiFiles() {
        return psiFiles;
    }

    @NotNull
    public static CodegenTestFiles create(@NotNull List<KtFile> ktFiles) {
        assert !ktFiles.isEmpty() : "List should have at least one file";
        return new CodegenTestFiles(ktFiles, Collections.emptyList(), Collections.emptyList());
    }

    public static CodegenTestFiles create(Project project, String[] names) {
        return create(project, names, KotlinTestUtils.getTestDataPathBase());
    }

    public static CodegenTestFiles create(Project project, String[] names, String testDataPath) {
        List<KtFile> files = new ArrayList<>(names.length);
        for (String name : names) {
            try {
                String content = KotlinTestUtils.doLoadFile(testDataPath + "/codegen/", name);
                KtFile file = KotlinTestUtils.createFile(name, content, project);
                files.add(file);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return create(files);
    }

    @NotNull
    public static CodegenTestFiles create(@NotNull String fileName, @NotNull String contentWithDiagnosticMarkup, @NotNull Project project) {
        String content = CheckerTestUtil.parseDiagnosedRanges(contentWithDiagnosticMarkup, new ArrayList<>());
        KtFile file = KotlinTestUtils.createFile(fileName, content, project);
        List<PsiErrorElement> ranges = AnalyzingUtils.getSyntaxErrorRanges(file);
        assert ranges.isEmpty() : "Syntax errors found in " + file + ": " + ranges;

        List<Pair<String, String>> expectedValues = Lists.newArrayList();

        Matcher matcher = Pattern.compile("// expected: (\\S+): (.*)").matcher(content);
        while (matcher.find()) {
            String fieldName = matcher.group(1);
            String expectedValue = matcher.group(2);
            expectedValues.add(Pair.create(fieldName, expectedValue));
        }

        List<Object> scriptParameterValues = Lists.newArrayList();

        if (file.isScript()) {
            Pattern scriptParametersPattern = Pattern.compile("param: (\\S.*)");
            Matcher scriptParametersMatcher = scriptParametersPattern.matcher(file.getText());

            if (scriptParametersMatcher.find()) {
                String valueString = scriptParametersMatcher.group(1);
                String[] values = valueString.split(" ");

                scriptParameterValues.add(values);
            } else {
                scriptParameterValues.add(ArrayUtil.EMPTY_STRING_ARRAY);
            }
        }

        KotlinScriptDefinitionProvider scriptDefinitionProvider = KotlinScriptDefinitionProvider.getInstance(project);
        assert scriptDefinitionProvider != null;
        scriptDefinitionProvider.addScriptDefinition(StandardScriptDefinition.INSTANCE);

        return new CodegenTestFiles(Collections.singletonList(file), expectedValues, scriptParameterValues);
    }
}
