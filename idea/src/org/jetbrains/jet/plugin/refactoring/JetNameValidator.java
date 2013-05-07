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

package org.jetbrains.jet.plugin.refactoring;

import com.intellij.openapi.project.Project;

import java.util.HashSet;
import java.util.Set;

/**
 * User: Alefas
 * Date: 07.02.12
 */
public abstract class JetNameValidator {
    private final Project project;

    protected JetNameValidator(Project project) {
        this.project = project;
    }

    public static JetNameValidator getEmptyValidator(final Project project) {
        return new JetNameValidator(project) {
            @Override
            protected boolean validateInner(String name) {
                return true;
            }
        };
    }

    public static JetNameValidator getCollectingValidator(final Project project) {
        return new JetNameValidator(project) {
            private final Set<String> suggestedSet = new HashSet<String>();

            @Override
            protected boolean validateInner(String name) {
                return !suggestedSet.contains(name);
            }

            @Override
            public String validateName(String name) {
                String validatedName = super.validateName(name);
                suggestedSet.add(validatedName);
                return validatedName;
            }
        };
    }

    /**
     * Validates name, and slightly improves it by adding number to name in case of conflicts
     * @param name to check it in scope
     * @return name or nameI, where I is number
     */
    public String validateName(String name) {
        if (validateInner(name)) return name;
        int i = 1;
        while (!validateInner(name + i)) {
            ++i;
        }

        return name + i;
    }

    protected abstract boolean validateInner(String name);

    public Project getProject() {
        return project;
    }
}
