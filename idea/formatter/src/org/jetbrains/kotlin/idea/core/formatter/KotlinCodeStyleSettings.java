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

package org.jetbrains.kotlin.idea.core.formatter;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.codeStyle.*;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.formatter.KotlinStyleGuideCodeStyle;
import org.jetbrains.kotlin.idea.util.ReflectionUtil;

import static com.intellij.util.ReflectionUtil.copyFields;

public class KotlinCodeStyleSettings extends CustomCodeStyleSettings {
    public static final KotlinCodeStyleSettings DEFAULT = new KotlinCodeStyleSettings(new CodeStyleSettings());

    public final PackageEntryTable PACKAGES_TO_USE_STAR_IMPORTS = new PackageEntryTable();
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
    public boolean SPACE_BEFORE_WHEN_PARENTHESES = true;
    public boolean LBRACE_ON_NEXT_LINE = false;
    public int NAME_COUNT_TO_USE_STAR_IMPORT = ApplicationManager.getApplication().isUnitTestMode() ? Integer.MAX_VALUE : 5;
    public int NAME_COUNT_TO_USE_STAR_IMPORT_FOR_MEMBERS = ApplicationManager.getApplication().isUnitTestMode() ? Integer.MAX_VALUE : 3;
    public boolean IMPORT_NESTED_CLASSES = false;
    public boolean CONTINUATION_INDENT_IN_PARAMETER_LISTS = true;
    public boolean CONTINUATION_INDENT_IN_ARGUMENT_LISTS = true;
    public boolean CONTINUATION_INDENT_FOR_EXPRESSION_BODIES = true;
    public boolean CONTINUATION_INDENT_FOR_CHAINED_CALLS = true;
    public boolean CONTINUATION_INDENT_IN_SUPERTYPE_LISTS = true;
    public boolean CONTINUATION_INDENT_IN_IF_CONDITIONS = true;
    public int BLANK_LINES_AROUND_BLOCK_WHEN_BRANCHES = 0;
    public int WRAP_EXPRESSION_BODY_FUNCTIONS = 0;
    public int WRAP_ELVIS_EXPRESSIONS = 1;
    public boolean IF_RPAREN_ON_NEW_LINE = false;

    @ReflectionUtil.SkipInEquals
    public String CODE_STYLE_DEFAULTS = null;

    private final boolean isTempForDeserialize;

    public KotlinCodeStyleSettings(CodeStyleSettings container) {
        this(container, false);
    }

    private KotlinCodeStyleSettings(CodeStyleSettings container, boolean isTempForDeserialize) {
        super("JetCodeStyleSettings", container);

        this.isTempForDeserialize = isTempForDeserialize;

        // defaults in IDE but not in tests
        if (!ApplicationManager.getApplication().isUnitTestMode()) {
            PACKAGES_TO_USE_STAR_IMPORTS.addEntry(new PackageEntry(false, "java.util", false));
            PACKAGES_TO_USE_STAR_IMPORTS.addEntry(new PackageEntry(false, "kotlinx.android.synthetic", true));
        }
    }

    public static KotlinCodeStyleSettings getInstance(Project project) {
        return CodeStyleSettingsManager.getSettings(project).getCustomSettings(KotlinCodeStyleSettings.class);
    }

    @Override
    public Object clone() {
        KotlinCodeStyleSettings clone = new KotlinCodeStyleSettings(getContainer());
        clone.copyFrom(this);
        return clone;
    }

    private void copyFrom(@NotNull KotlinCodeStyleSettings from) {
        copyFields(getClass().getFields(), from, this);
        PACKAGES_TO_USE_STAR_IMPORTS.copyFrom(from.PACKAGES_TO_USE_STAR_IMPORTS);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof KotlinCodeStyleSettings)) return false;
        if (!ReflectionUtil.comparePublicNonFinalFieldsWithSkip(this, obj)) return false;
        return true;
    }

    @Override
    public void writeExternal(Element parentElement, @NotNull CustomCodeStyleSettings parentSettings) throws WriteExternalException {
        if (KotlinStyleGuideCodeStyle.CODE_STYLE_ID.equals(CODE_STYLE_DEFAULTS)) {
            KotlinCodeStyleSettings defaultKotlinCodeStyle = (KotlinCodeStyleSettings) parentSettings.clone();
            KotlinStyleGuideCodeStyle.Companion.applyToKotlinCustomSettings(defaultKotlinCodeStyle, false);
            parentSettings = defaultKotlinCodeStyle;
        }

        super.writeExternal(parentElement, parentSettings);
    }

    @Override
    public void readExternal(Element parentElement) throws InvalidDataException {
        if (isTempForDeserialize) {
            super.readExternal(parentElement);
            return;
        }

        KotlinCodeStyleSettings tempSettings = readExternalToTemp(parentElement);
        if (KotlinStyleGuideCodeStyle.CODE_STYLE_ID.equals(tempSettings.CODE_STYLE_DEFAULTS)) {
            KotlinStyleGuideCodeStyle.Companion.applyToKotlinCustomSettings(this, true);
        }

        // Actual read
        super.readExternal(parentElement);
    }

    private static KotlinCodeStyleSettings readExternalToTemp(Element parentElement) {
        // Read to temp
        KotlinCodeStyleSettings tempSettings = new KotlinCodeStyleSettings(null, true);
        tempSettings.readExternal(parentElement);

        return tempSettings;
    }
}
