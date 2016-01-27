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

package org.jetbrains.kotlin.idea.actions.generate;

import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.java.generate.template.toString.ToStringTemplatesManager;

import javax.swing.*;
import java.awt.*;

public class ToStringMemberChooserHeaderPanel extends JPanel {
    private final JComboBox comboBox;
    private final JCheckBox generateSuperCheckBox;

    public ToStringMemberChooserHeaderPanel(boolean allowSuperCall) {
        super(new GridBagLayout());

        comboBox = new ComboBox(KotlinGenerateToStringAction.Generator.values());
        comboBox.setRenderer(
                new DefaultListCellRenderer() {
                    @NotNull
                    @Override
                    public Component getListCellRendererComponent(
                            JList list,
                            Object value,
                            int index,
                            boolean isSelected,
                            boolean cellHasFocus
                    ) {
                        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                        setText(((KotlinGenerateToStringAction.Generator) value).getText());
                        return this;
                    }
                }
        );
        comboBox.setSelectedItem(ToStringTemplatesManager.getInstance().getDefaultTemplate());

        JLabel templatesLabel = new JLabel("Choose implementation: ");
        templatesLabel.setDisplayedMnemonic('i');
        templatesLabel.setLabelFor(comboBox);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.BASELINE;
        constraints.gridx = 0;
        add(templatesLabel, constraints);

        constraints.gridx = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        add(comboBox, constraints);

        if (allowSuperCall) {
            generateSuperCheckBox = new JCheckBox("Generate call to super.toString()");
            generateSuperCheckBox.setMnemonic('s');
            constraints.gridx = 2;
            constraints.weightx = 0.0;
            add(generateSuperCheckBox, constraints);
        }
        else {
            generateSuperCheckBox = null;
        }
    }

    public KotlinGenerateToStringAction.Generator getSelectedGenerator() {
        return (KotlinGenerateToStringAction.Generator) comboBox.getSelectedItem();
    }

    public boolean isGenerateSuperCall() {
        return generateSuperCheckBox != null && generateSuperCheckBox.isSelected();
    }
}