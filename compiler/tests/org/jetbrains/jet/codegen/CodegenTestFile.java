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

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.ref.JetTypeName;

import java.util.List;
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
    @NotNull
    private final List<AnalyzerScriptParameter> scriptParameterTypes;
    @NotNull
    private final List<Object> scriptParameterValues;

    private CodegenTestFile(
            @NotNull JetFile psiFile,
            @NotNull String expectedValue,
            @NotNull List<AnalyzerScriptParameter> scriptParameterTypes,
            @NotNull List<Object> scriptParameterValues) {
        this.psiFile = psiFile;
        this.expectedValue = expectedValue;
        this.scriptParameterTypes = scriptParameterTypes;
        this.scriptParameterValues = scriptParameterValues;
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
    public List<AnalyzerScriptParameter> getScriptParameterTypes() {
        return scriptParameterTypes;
    }

    @NotNull
    public List<Object> getScriptParameterValues() {
        return scriptParameterValues;
    }

    @NotNull
    public static CodegenTestFile create(@NotNull String fileName, @NotNull String content, @NotNull Project project) {
        JetFile file = (JetFile) JetTestUtils.createFile(fileName, content, project);

        Matcher matcher = Pattern.compile("// expected: (.*)").matcher(content);
        String expectedValue = matcher.find() ? matcher.group(1) : "OK";

        List<AnalyzerScriptParameter> scriptParameterTypes = Lists.newArrayList();
        List<Object> scriptParameterValues = Lists.newArrayList();

        if (file.isScript()) {
            Pattern scriptParametersPattern = Pattern.compile("param: (\\S+): (\\S+): (\\S.*)");
            Matcher scriptParametersMatcher = scriptParametersPattern.matcher(file.getText());

            while (scriptParametersMatcher.find()) {
                String name = scriptParametersMatcher.group(1);
                String type = scriptParametersMatcher.group(2);
                String valueString = scriptParametersMatcher.group(3);
                Object value;

                if (type.equals("jet.String")) {
                    value = valueString;
                }
                else if (type.equals("jet.Long")) {
                    value = Long.parseLong(valueString);
                }
                else if (type.equals("jet.Int")) {
                    value = Integer.parseInt(valueString);
                }
                else if (type.equals("jet.Array<jet.String>")) {
                    value = valueString.split(" ");
                }
                else {
                    throw new AssertionError("TODO");
                }

                scriptParameterTypes.add(new AnalyzerScriptParameter(Name.identifier(name), JetTypeName.parse(type)));
                scriptParameterValues.add(value);
            }
        }

        return new CodegenTestFile(file, expectedValue, scriptParameterTypes, scriptParameterValues);
    }
}
