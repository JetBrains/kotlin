/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.ref.JetTypeName;
import org.jetbrains.jet.parsing.JetParsingTest;

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
    private final List<AnalyzerScriptParameter> scriptParameterTypes;
    @NotNull
    private final List<Object> scriptParameterValues;

    private CodegenTestFiles(
            @NotNull List<JetFile> psiFiles,
            @NotNull List<Pair<String, String>> expectedValues,
            @NotNull List<AnalyzerScriptParameter> scriptParameterTypes,
            @NotNull List<Object> scriptParameterValues) {
        this.psiFiles = psiFiles;
        this.expectedValues = expectedValues;
        this.scriptParameterTypes = scriptParameterTypes;
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
    public List<AnalyzerScriptParameter> getScriptParameterTypes() {
        return scriptParameterTypes;
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
        ArrayList<JetFile> files = new ArrayList<JetFile>();
        for (String name : names) {
            try {
                String content = JetTestUtils.doLoadFile(JetParsingTest.getTestDataDir() + "/codegen/", name);
                int i = name.lastIndexOf('/');
                //name = name.substring(i+1);
                JetFile file = JetTestUtils.createFile(project, name, content);
                files.add(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new CodegenTestFiles(files, Collections.<Pair<String, String>>emptyList(), Collections.<AnalyzerScriptParameter>emptyList(), Collections.emptyList());
    }

    @NotNull
    public static CodegenTestFiles create(@NotNull String fileName, @NotNull String content, @NotNull Project project) {
        JetFile file = JetTestUtils.createFile(project, fileName, content);

        List<Pair<String, String>> expectedValues = Lists.newArrayList();

        Matcher matcher = Pattern.compile("// expected: (\\S+): (.*)").matcher(content);
        while (matcher.find()) {
            String fieldName = matcher.group(1);
            String expectedValue = matcher.group(2);
            expectedValues.add(Pair.create(fieldName, expectedValue));
        }

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

        return new CodegenTestFiles(Collections.singletonList(file), expectedValues, scriptParameterTypes, scriptParameterValues);
    }
}
