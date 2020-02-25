/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor;

import com.intellij.openapi.options.BeanConfigurable;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.ui.IdeBorderFactory;
import org.jetbrains.kotlin.idea.KotlinBundle;

import javax.swing.*;

public class KotlinEditorOptionsConfigurable extends BeanConfigurable<KotlinEditorOptions> implements UnnamedConfigurable {
    public KotlinEditorOptionsConfigurable() {
        super(KotlinEditorOptions.getInstance());
        KotlinEditorOptions instance = KotlinEditorOptions.getInstance();
        checkBox(KotlinBundle.message("editor.checkbox.title.convert.pasted.java.code.to.kotlin"),
                 instance::isEnableJavaToKotlinConversion,
                 instance::setEnableJavaToKotlinConversion);
        checkBox(KotlinBundle.message("editor.checkbox.title.don.t.show.java.to.kotlin.conversion.dialog.on.paste"),
                 instance::isDonTShowConversionDialog,
                 instance::setDonTShowConversionDialog);
        checkBox(KotlinBundle.message("editor.checkbox.title.auto.add.val.keyword.to.data.inline.class.constructor.parameters"),
                 instance::isAutoAddValKeywordToDataClassParameters,
                 instance::setAutoAddValKeywordToDataClassParameters);
    }

    @Override
    public JComponent createComponent() {
        JComponent component = super.createComponent();
        component.setBorder(IdeBorderFactory.createTitledBorder(KotlinBundle.message("editor.title.kotlin")));
        return component;
    }
}
