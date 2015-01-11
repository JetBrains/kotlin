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

package org.jetbrains.kotlin.parsing;

import org.jetbrains.kotlin.resolve.AnalyzerScriptParameter;
import org.jetbrains.kotlin.resolve.ImportPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JetScriptDefinition {
    private final String extension;
    private final List<AnalyzerScriptParameter> parameters;

    public JetScriptDefinition(String extension, List<AnalyzerScriptParameter> scriptParameters) {
        this.extension = extension;
        parameters = scriptParameters == null ? Collections.<AnalyzerScriptParameter>emptyList() : scriptParameters;
    }

    private static List<ImportPath> importPaths(List<String> imports) {
        List<ImportPath> paths = new ArrayList<ImportPath>(imports.size());
        for (String anImport : imports) {
            paths.add(new ImportPath(anImport));
        }
        return paths;
    }

    public JetScriptDefinition(String extension, AnalyzerScriptParameter... scriptParameters) {
        this(extension, Arrays.asList(scriptParameters));
    }

    public List<AnalyzerScriptParameter> getScriptParameters() {
        return parameters;
    }

    public String getExtension() {
        return extension;
    }
}
