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

package org.jetbrains.jet.plugin.formatter;

import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

public class JetCodeStyleSettings extends CustomCodeStyleSettings {

    public boolean SPACE_AROUND_RANGE = false;

    public boolean SPACE_BEFORE_TYPE_COLON = false;
    public boolean SPACE_AFTER_TYPE_COLON = true;

    public boolean SPACE_BEFORE_EXTEND_COLON = false;
    public boolean SPACE_AFTER_EXTEND_COLON = true;

    public boolean INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD = true;
    public boolean ALIGN_IN_COLUMNS_CASE_BRANCH = false;

    public static JetCodeStyleSettings getInstance(Project project) {
        return CodeStyleSettingsManager.getSettings(project).getCustomSettings(JetCodeStyleSettings.class);
    }

    public JetCodeStyleSettings(CodeStyleSettings container) {
        super("JetCodeStyleSettings", container);
    }
}
