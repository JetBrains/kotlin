/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight;

import com.intellij.application.options.editor.AutoImportOptionsProvider;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.statistics.FUSEventGroups;
import org.jetbrains.kotlin.idea.statistics.KotlinFUSLogger;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

public class KotlinCodeInsightWorkspaceSettingsProvider implements AutoImportOptionsProvider {
    private final Project project;

    private JPanel myPanel;
    private JCheckBox myOptimizeImportsOnTheFly;
    private JCheckBox myAddUnambiguousImportsOnTheFly;

    public KotlinCodeInsightWorkspaceSettingsProvider(Project project) {
        this.project = project;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return myPanel;
    }

    private KotlinCodeInsightSettings settings() {
        return KotlinCodeInsightSettings.Companion.getInstance();
    }

    private KotlinCodeInsightWorkspaceSettings projectSettings() {
        return KotlinCodeInsightWorkspaceSettings.Companion.getInstance(project);
    }

    @Override
    public boolean isModified() {
        KotlinCodeInsightSettings settings = settings();
        KotlinCodeInsightWorkspaceSettings projectSettings = projectSettings();
        return projectSettings.optimizeImportsOnTheFly != myOptimizeImportsOnTheFly.isSelected()
               || settings.addUnambiguousImportsOnTheFly != myAddUnambiguousImportsOnTheFly.isSelected();
    }

    @Override
    public void apply() throws ConfigurationException {
        KotlinCodeInsightSettings settings = settings();
        KotlinCodeInsightWorkspaceSettings projectSettings = projectSettings();

        projectSettings.optimizeImportsOnTheFly = myOptimizeImportsOnTheFly.isSelected();
        settings.addUnambiguousImportsOnTheFly = myAddUnambiguousImportsOnTheFly.isSelected();

        final Map<String, String> data = new HashMap<>();
        data.put("optimizeImportsOnTheFly", Boolean.toString(projectSettings.optimizeImportsOnTheFly));
        data.put("addUnambiguousImportsOnTheFly", Boolean.toString(settings.addUnambiguousImportsOnTheFly));

        KotlinFUSLogger.Companion.log(FUSEventGroups.Settings, "KotlinCodeInsightWorkspaceSettings", data);
    }

    @Override
    public void reset() {
        KotlinCodeInsightSettings settings = settings();
        KotlinCodeInsightWorkspaceSettings projectSettings = projectSettings();
        myOptimizeImportsOnTheFly.setSelected(projectSettings.optimizeImportsOnTheFly);
        myAddUnambiguousImportsOnTheFly.setSelected(settings.addUnambiguousImportsOnTheFly);
    }
}
