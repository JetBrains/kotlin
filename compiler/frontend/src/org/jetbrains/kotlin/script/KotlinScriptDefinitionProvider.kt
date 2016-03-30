/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.script;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class KotlinScriptDefinitionProvider {
    private final List<KotlinScriptDefinition> definitions = new ArrayList<KotlinScriptDefinition>();

    public static KotlinScriptDefinitionProvider getInstance(Project project) {
        return ServiceManager.getService(project, KotlinScriptDefinitionProvider.class);
    }

    public KotlinScriptDefinitionProvider() {
        definitions.add(StandardScriptDefinition.INSTANCE);
    }

    public KotlinScriptDefinition findScriptDefinition(PsiFile psiFile) {
        for (KotlinScriptDefinition definition : definitions) {
            if (definition.isScript(psiFile)) {
                return definition;
            }
        }

        return null;
    }

    public boolean isScript(PsiFile psiFile) {
        return findScriptDefinition(psiFile) != null;
    }

    public void addScriptDefinition(@NotNull KotlinScriptDefinition scriptDefinition) {
        definitions.add(0, scriptDefinition);
    }

    public void removeScriptDefinition(@NotNull KotlinScriptDefinition scriptDefinition) {
        definitions.remove(scriptDefinition);
    }

    public void setScriptDefinitions(@NotNull List<KotlinScriptDefinition> definitions) {
        this.definitions.clear();
        this.definitions.addAll(definitions);
    }
}
