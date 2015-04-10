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

package org.jetbrains.kotlin.idea.core.formatter;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.*;
import com.intellij.util.ReflectionUtil;
import org.jetbrains.annotations.NotNull;

public class JetCodeStyleSettings extends CustomCodeStyleSettings {

    public boolean SPACE_AROUND_RANGE = false;

    public boolean SPACE_BEFORE_TYPE_COLON = false;
    public boolean SPACE_AFTER_TYPE_COLON = true;

    public boolean SPACE_BEFORE_EXTEND_COLON = true;
    public boolean SPACE_AFTER_EXTEND_COLON = true;

    public boolean INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD = true;
    public boolean ALIGN_IN_COLUMNS_CASE_BRANCH = false;

    public boolean SPACE_AROUND_FUNCTION_TYPE_ARROW = true;

    public boolean SPACE_AROUND_WHEN_ARROW = true;
    public boolean SPACE_BEFORE_LAMBDA_ARROW = true;

    public boolean LBRACE_ON_NEXT_LINE = false;

    public int NAME_COUNT_TO_USE_STAR_IMPORT = ApplicationManager.getApplication().isUnitTestMode() ? Integer.MAX_VALUE : 5;
    public boolean IMPORT_PACKAGES = true;
    public boolean IMPORT_NESTED_CLASSES = false;
    public final PackageEntryTable PACKAGES_TO_USE_STAR_IMPORTS = new PackageEntryTable();

    public static JetCodeStyleSettings getInstance(Project project) {
        return CodeStyleSettingsManager.getSettings(project).getCustomSettings(JetCodeStyleSettings.class);
    }

    public JetCodeStyleSettings(CodeStyleSettings container) {
        super("JetCodeStyleSettings", container);

        // defaults in IDE but not in tests
        if (!ApplicationManager.getApplication().isUnitTestMode()) {
            PACKAGES_TO_USE_STAR_IMPORTS.addEntry(new PackageEntry(false, "java.util", false));
        }
    }

    @Override
    public Object clone() {
        JetCodeStyleSettings clone = new JetCodeStyleSettings(getContainer());
        clone.copyFrom(this);
        return clone;
    }

    private void copyFrom(@NotNull JetCodeStyleSettings from) {
        ReflectionUtil.copyFields(getClass().getFields(), from, this);

        PACKAGES_TO_USE_STAR_IMPORTS.copyFrom(from.PACKAGES_TO_USE_STAR_IMPORTS);
    }
}
