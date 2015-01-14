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

package org.jetbrains.kotlin.idea.debugger;


import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class KotlinSteppingConfigurableUi implements SearchableConfigurable {
    private JCheckBox ignoreKotlinMethods;
    private JPanel myPanel;
    private boolean isModified = false;
    private final KotlinDebuggerSettings mySettings;

    public KotlinSteppingConfigurableUi(KotlinDebuggerSettings settings) {
        mySettings = settings;
        ignoreKotlinMethods.setSelected(settings.getDEBUG_DISABLE_KOTLIN_INTERNAL_CLASSES());

        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(@NotNull ActionEvent e) {
                isModified = mySettings.getDEBUG_DISABLE_KOTLIN_INTERNAL_CLASSES() != ignoreKotlinMethods.isSelected();
            }
        };
        ignoreKotlinMethods.addActionListener(listener);
    }

    @Override
    public String getDisplayName() {
        return "Kotlin";
    }

    @Override
    @NotNull
    public String getHelpTopic() {
        return "reference.idesettings.debugger.kotlin";
    }

    @Override
    @NotNull
    public String getId() {
        return getHelpTopic();
    }

    @Override
    public Runnable enableSearch(String option) {
        return null;
    }

    @Override
    public JComponent createComponent() {
        return myPanel;
    }

    @Override
    public boolean isModified() {
        return isModified;
    }

    @Override
    public void apply() throws ConfigurationException {
        if (isModified) {
            mySettings.setDEBUG_DISABLE_KOTLIN_INTERNAL_CLASSES(ignoreKotlinMethods.isSelected());
        }
        isModified = false;
    }

    @Override
    public void reset() {
        ignoreKotlinMethods.setSelected(mySettings.getDEBUG_DISABLE_KOTLIN_INTERNAL_CLASSES());
    }

    @Override
    public void disposeUIResources() {
    }
}
