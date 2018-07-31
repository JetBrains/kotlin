/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.script.configuration;

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
import org.jetbrains.kotlin.script.KotlinScriptDefinition;

import javax.swing.*;
import java.awt.*;

public class KotlinScriptingSettingsConfigurable implements SearchableConfigurable, Configurable.NoScroll {
    public static final String ID = "preferences.language.Kotlin.scripting";

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
        model = KotlinScriptDefinitionsModel.Companion.createModel(manager.getAllDefinitions(), settings);

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
               || isScriptDefinitionsChanged();
    }

    @Override
    public void apply() {
        settings.setAutoReloadEnabled(scriptDependenciesAutoReload.isSelected());

        if (isScriptDefinitionsChanged()) {
            for (KotlinScriptDefinitionsModelDescriptor item : model.getItems()) {
                KotlinScriptDefinition definition = item.getDefinition();
                settings.setOrder(definition, model.getItems().indexOf(item));
                settings.setEnabled(definition, item.isEnabled());
            }

            manager.reorderScriptDefinitions();
        }
    }

    @Override
    public void reset() {
        scriptDependenciesAutoReload.setSelected(settings.isAutoReloadEnabled());

        model.setDefinitions(manager.getAllDefinitions(), settings);
    }

    private boolean isScriptDefinitionsChanged() {
        for (KotlinScriptDefinitionsModelDescriptor item : model.getItems()) {
            if (settings.isScriptDefinitionEnabled(item.getDefinition()) != item.isEnabled()) {
                return true;
            }
        }
        return !model.getDefinitions().equals(manager.getAllDefinitions());
    }


    @Override
    @Nls
    public String getDisplayName() {
        return "Kotlin Scripting";
    }

    @Override
    @NotNull
    public String getId() {
        return ID;
    }
}
