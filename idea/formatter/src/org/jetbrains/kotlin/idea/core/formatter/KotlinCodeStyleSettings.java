/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.formatter;

import com.intellij.application.options.CodeStyle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;
import com.intellij.psi.codeStyle.PackageEntry;
import com.intellij.psi.codeStyle.PackageEntryTable;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.formatter.KotlinObsoleteCodeStyle;
import org.jetbrains.kotlin.idea.formatter.KotlinStyleGuideCodeStyle;
import org.jetbrains.kotlin.idea.util.FormatterUtilKt;
import org.jetbrains.kotlin.idea.util.ReflectionUtil;

import static com.intellij.util.ReflectionUtil.copyFields;

public class KotlinCodeStyleSettings extends CustomCodeStyleSettings {
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
    public boolean CONTINUATION_INDENT_IN_ELVIS = true;
    public int BLANK_LINES_AROUND_BLOCK_WHEN_BRANCHES = 0;
    public int WRAP_EXPRESSION_BODY_FUNCTIONS = 0;
    public int WRAP_ELVIS_EXPRESSIONS = 1;
    public boolean IF_RPAREN_ON_NEW_LINE = false;
    public boolean ALLOW_TRAILING_COMMA = false;

    @ReflectionUtil.SkipInEquals
    public String CODE_STYLE_DEFAULTS = null;

    /**
     * Load settings with previous IDEA defaults to have an ability to restore them.
     */
    @Nullable
    private KotlinCodeStyleSettings settingsAgainstPreviousDefaults = null;

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
        return CodeStyle.getSettings(project).getCustomSettings(KotlinCodeStyleSettings.class);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Object clone() {
        return cloneSettings();
    }

    @NotNull
    public KotlinCodeStyleSettings cloneSettings() {
        KotlinCodeStyleSettings clone = new KotlinCodeStyleSettings(getContainer());
        clone.copyFrom(this);
        clone.settingsAgainstPreviousDefaults = this.settingsAgainstPreviousDefaults;
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
        if (CODE_STYLE_DEFAULTS != null) {
            KotlinCodeStyleSettings defaultKotlinCodeStyle = (KotlinCodeStyleSettings) parentSettings.clone();

            if (KotlinStyleGuideCodeStyle.CODE_STYLE_ID.equals(CODE_STYLE_DEFAULTS)) {
                KotlinStyleGuideCodeStyle.Companion.applyToKotlinCustomSettings(defaultKotlinCodeStyle, false);
            }
            else if (KotlinObsoleteCodeStyle.CODE_STYLE_ID.equals(CODE_STYLE_DEFAULTS)) {
                KotlinObsoleteCodeStyle.Companion.applyToKotlinCustomSettings(defaultKotlinCodeStyle, false);
            }

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
        String customDefaults = tempSettings.CODE_STYLE_DEFAULTS;

        if (KotlinStyleGuideCodeStyle.CODE_STYLE_ID.equals(customDefaults)) {
            KotlinStyleGuideCodeStyle.Companion.applyToKotlinCustomSettings(this, true);
        }
        else if (KotlinObsoleteCodeStyle.CODE_STYLE_ID.equals(customDefaults)) {
            KotlinObsoleteCodeStyle.Companion.applyToKotlinCustomSettings(this, true);
        }
        else if (customDefaults == null && FormatterUtilKt.isDefaultOfficialCodeStyle()) {
            // Temporary load settings against previous defaults
            settingsAgainstPreviousDefaults = new KotlinCodeStyleSettings(null, true);
            KotlinObsoleteCodeStyle.Companion.applyToKotlinCustomSettings(settingsAgainstPreviousDefaults, true);
            settingsAgainstPreviousDefaults.readExternal(parentElement);
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

    public boolean canRestore() {
        return settingsAgainstPreviousDefaults != null;
    }

    public void restore() {
        if (settingsAgainstPreviousDefaults != null) {
            copyFrom(settingsAgainstPreviousDefaults);
        }
    }

    public static KotlinCodeStyleSettings defaultSettings() {
        return ServiceManager.getService(KotlinCodeStyleSettingsHolder.class).defaultSettings;
    }

    public static final class KotlinCodeStyleSettingsHolder {
        private final KotlinCodeStyleSettings defaultSettings = new KotlinCodeStyleSettings(new CodeStyleSettings());
    }
}
