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

package org.jetbrains.kotlin.idea.editor;

import com.intellij.openapi.options.BeanConfigurable;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.ui.IdeBorderFactory;

import javax.swing.*;

public class KotlinEditorOptionsConfigurable extends BeanConfigurable<KotlinEditorOptions> implements UnnamedConfigurable {
    public KotlinEditorOptionsConfigurable() {
        super(KotlinEditorOptions.getInstance());
        checkBox("enableJavaToKotlinConversion", "Convert pasted Java code to Kotlin");
        checkBox("donTShowConversionDialog", "Don't show Java to Kotlin conversion dialog on paste");
    }

    @Override
    public JComponent createComponent() {
        JComponent component = super.createComponent();
        component.setBorder(IdeBorderFactory.createTitledBorder("Kotlin"));
        return component;
    }
}
