/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.script.configuration;

import com.google.common.collect.Lists;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionsManager;
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings;

import javax.swing.*;
import java.awt.*;

public class KotlinScriptingSettingsConfigurable implements SearchableConfigurable, Configurable.NoScroll {
    private JPanel root;
    private JPanel panelScriptDefinitionsChooser;
    private JCheckBox scriptDependenciesAutoReload;

    private KotlinScriptDefinitionsModel model;

    private final ScriptDefinitionsManager manager;
    private final KotlinScriptingSettings settings;

    public KotlinScriptingSettingsConfigurable(Project project) {
        manager = ScriptDefinitionsManager.Companion.getInstance(project);
        settings = KotlinScriptingSettings.Companion.getInstance(project);
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        model = new KotlinScriptDefinitionsModel(Lists.newArrayList(manager.getAllDefinitions()));

        panelScriptDefinitionsChooser.setLayout(new BorderLayout());
        panelScriptDefinitionsChooser.add(
                ToolbarDecorator.createDecorator(new TableView<>(model))
                        .disableAddAction()
                        .disableRemoveAction()
                        .createPanel()
        );

        return root;
    }

    @Override
    public boolean isModified() {
        return isModified(scriptDependenciesAutoReload, settings.isAutoReloadEnabled())
               || !model.getItems().equals(manager.getAllDefinitions());
    }

    @Override
    public void apply() {
        settings.setAutoReloadEnabled(scriptDependenciesAutoReload.isSelected());


        // todo
    }

    @Override
    public void reset() {
        scriptDependenciesAutoReload.setSelected(settings.isAutoReloadEnabled());

        // todo
    }

    @Override
    @Nls
    public String getDisplayName() {
        return "Kotlin Scripting";
    }

    @Override
    @NotNull
    public String getId() {
        return "preferences.language.Kotlin.scripting";
    }
}
