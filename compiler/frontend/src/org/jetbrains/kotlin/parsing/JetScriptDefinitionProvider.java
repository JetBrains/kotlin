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

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.AnalyzerScriptParameter;

import java.util.*;

public class JetScriptDefinitionProvider {
    private final Map<String, JetScriptDefinition> scripts = new HashMap<String, JetScriptDefinition>();
    private final Set<PsiFile> scriptsFiles = new HashSet<PsiFile>();

    private static final JetScriptDefinition standardScript = new JetScriptDefinition(JetParserDefinition.STD_SCRIPT_EXT, Collections.<AnalyzerScriptParameter>emptyList());

    public static JetScriptDefinitionProvider getInstance(Project project) {
        return ServiceManager.getService(project, JetScriptDefinitionProvider.class);
    }

    public JetScriptDefinitionProvider() {
        // .kts will take analyzer parameters explicitly specified on compilation
        addScriptDefinition(standardScript);
    }

    public void markFileAsScript(JetFile file) {
        scriptsFiles.add(file);
    }

    public JetScriptDefinition findScriptDefinition(PsiFile psiFile) {
        boolean force = scriptsFiles.contains(psiFile);

        String name = psiFile.getName();
        for (Map.Entry<String, JetScriptDefinition> e : scripts.entrySet()) {
            if (name.endsWith(e.getKey())) {
                return e.getValue();
            }
        }
        if(force)
            return standardScript;

        return null;
    }

    public boolean isScript(PsiFile psiFile) {
        return findScriptDefinition(psiFile) != null;
    }

    public void addScriptDefinition(@NotNull JetScriptDefinition scriptDefinition) {
        scripts.put(scriptDefinition.getExtension(), scriptDefinition);
    }

    public void addScriptDefinitions(List<JetScriptDefinition> definitions) {
        for (JetScriptDefinition definition : definitions) {
            addScriptDefinition(definition);
        }
    }
}
