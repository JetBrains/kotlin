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

package org.jetbrains.kotlin.idea.framework.ui;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

public class ChooseModulePanel {
    private JPanel contentPane;
    private JRadioButton allModulesWithKtRadioButton;
    private JRadioButton singleModuleRadioButton;
    private JComboBox singleModuleComboBox;
    private JTextField allModulesNames;

    @NotNull private final Project project;
    @NotNull private final List<Module> modules;

    public ChooseModulePanel(@NotNull Project project, @NotNull List<Module> modules) {
        this.project = project;
        this.modules = modules;

        DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();

        for (Module module : modules) {
            comboBoxModel.addElement(module.getName());
        }

        singleModuleComboBox.setModel(comboBoxModel);
        singleModuleRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateComponents();
            }
        });
        allModulesWithKtRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateComponents();
            }
        });

        String fullList = StringUtil.join(modules, new Function<Module, String>() {
            @Override
            public String fun(Module module) {
                return module.getName();
            }
        }, ", ");
        allModulesNames.setText(fullList);
        allModulesNames.setBorder(null);

        ButtonGroup modulesGroup = new ButtonGroup();
        modulesGroup.add(allModulesWithKtRadioButton);
        modulesGroup.add(singleModuleRadioButton);
        allModulesWithKtRadioButton.setSelected(true);

        updateComponents();
    }

    public JComponent getContentPane() {
        return contentPane;
    }

    private void updateComponents() {
        singleModuleComboBox.setEnabled(singleModuleRadioButton.isSelected());
    }

    public List<Module> getModulesToConfigure() {
        if (allModulesWithKtRadioButton.isSelected()) return modules;
        String selectedItem = (String) singleModuleComboBox.getSelectedItem();
        return Collections.singletonList(ModuleManager.getInstance(project).findModuleByName(selectedItem));
    }
}
