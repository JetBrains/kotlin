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

package org.jetbrains.kotlin.codegen;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiErrorElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.checkers.CheckerTestUtil;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.parsing.JetScriptDefinition;
import org.jetbrains.kotlin.parsing.JetScriptDefinitionProvider;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.AnalyzerScriptParameter;
import org.jetbrains.kotlin.resolve.AnalyzingUtils;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.Variance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodegenTestFiles {

    @NotNull
    private final List<JetFile> psiFiles;
    @NotNull
    private final List<Pair<String, String>> expectedValues;
    @NotNull
    private final List<Object> scriptParameterValues;

    private CodegenTestFiles(
            @NotNull List<JetFile> psiFiles,
            @NotNull List<Pair<String, String>> expectedValues,
            @NotNull List<Object> scriptParameterValues
    ) {
        this.psiFiles = psiFiles;
        this.expectedValues = expectedValues;
        this.scriptParameterValues = scriptParameterValues;
    }

    @NotNull
    public JetFile getPsiFile() {
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
    public List<JetFile> getPsiFiles() {
        return psiFiles;
    }

    public static CodegenTestFiles create(Project project, String[] names) {
        return create(project, names, JetTestUtils.getTestDataPathBase());
    }

    public static CodegenTestFiles create(Project project, String[] names, String testDataPath) {
        ArrayList<JetFile> files = new ArrayList<JetFile>();
        for (String name : names) {
            try {
                String content = JetTestUtils.doLoadFile(testDataPath + "/codegen/", name);
                int i = name.lastIndexOf('/');
                //name = name.substring(i+1);
                JetFile file = JetTestUtils.createFile(name, content, project);
                files.add(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new CodegenTestFiles(files, Collections.<Pair<String, String>>emptyList(), Collections.emptyList());
    }

    @NotNull
    public static CodegenTestFiles create(@NotNull String fileName, @NotNull String contentWithDiagnosticMarkup, @NotNull Project project) {
        String content = CheckerTestUtil.parseDiagnosedRanges(contentWithDiagnosticMarkup, new ArrayList<CheckerTestUtil.DiagnosedRange>());
        JetFile file = JetTestUtils.createFile(fileName, content, project);
        List<PsiErrorElement> ranges = AnalyzingUtils.getSyntaxErrorRanges(file);
        assert ranges.isEmpty() : "Syntax errors found in " + file + ": " + ranges;

        List<Pair<String, String>> expectedValues = Lists.newArrayList();

        Matcher matcher = Pattern.compile("// expected: (\\S+): (.*)").matcher(content);
        while (matcher.find()) {
            String fieldName = matcher.group(1);
            String expectedValue = matcher.group(2);
            expectedValues.add(Pair.create(fieldName, expectedValue));
        }

        List<AnalyzerScriptParameter> scriptParameterTypes = Lists.newArrayList();
        List<Object> scriptParameterValues = Lists.newArrayList();

        // SCRIPT: extract script params, move to ScriptingUtil
        if (file.isScript()) {
            Pattern scriptParametersPattern = Pattern.compile("param: (\\S+): (\\S+): (\\S.*)");
            Matcher scriptParametersMatcher = scriptParametersPattern.matcher(file.getText());

            while (scriptParametersMatcher.find()) {
                String name = scriptParametersMatcher.group(1);
                String type = scriptParametersMatcher.group(2);
                String valueString = scriptParametersMatcher.group(3);
                Object value;

                JetType jetType;
                KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
                if (type.equals("kotlin.String")) {
                    value = valueString;
                    jetType = builtIns.getStringType();
                }
                else if (type.equals("kotlin.Long")) {
                    value = Long.parseLong(valueString);
                    jetType = builtIns.getLongType();
                }
                else if (type.equals("kotlin.Int")) {
                    value = Integer.parseInt(valueString);
                    jetType = builtIns.getIntType();
                }
                else if (type.equals("kotlin.Array<kotlin.String>")) {
                    value = valueString.split(" ");
                    jetType = builtIns.getArrayType(Variance.INVARIANT, builtIns.getStringType());
                }
                else {
                    throw new AssertionError("TODO: " + type);
                }

                scriptParameterTypes.add(new AnalyzerScriptParameter(Name.identifier(name), jetType));
                scriptParameterValues.add(value);
            }

            JetScriptDefinitionProvider.getInstance(project).addScriptDefinition(
                    new JetScriptDefinition(
                            ".kts",
                            scriptParameterTypes
                    )
            );
        }

        return new CodegenTestFiles(Collections.singletonList(file), expectedValues, scriptParameterValues);
    }
}
