/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor;

import com.intellij.openapi.options.BeanConfigurable;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.ui.IdeBorderFactory;

import javax.swing.*;

public class KotlinEditorOptionsConfigurable extends BeanConfigurable<KotlinEditorOptions> implements UnnamedConfigurable {
    public KotlinEditorOptionsConfigurable() {
        super(KotlinEditorOptions.getInstance());
        KotlinEditorOptions instance = KotlinEditorOptions.getInstance();
        checkBox("Convert pasted Java code to Kotlin",
                 instance::isEnableJavaToKotlinConversion,
                 instance::setEnableJavaToKotlinConversion);
        checkBox("Don't show Java to Kotlin conversion dialog on paste",
                 instance::isDonTShowConversionDialog,
                 instance::setDonTShowConversionDialog);
        checkBox("Auto add val keyword to data/inline class constructor parameters",
                 instance::isAutoAddValKeywordToDataClassParameters,
                 instance::setAutoAddValKeywordToDataClassParameters);
    }

    @Override
    public JComponent createComponent() {
        JComponent component = super.createComponent();
        component.setBorder(IdeBorderFactory.createTitledBorder("Kotlin"));
        return component;
    }
}
