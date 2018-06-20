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

package org.jetbrains.kotlin.idea.findUsages.dialogs;

import com.intellij.find.FindBundle;
import com.intellij.find.FindSettings;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.JavaFindUsagesDialog;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.StateRestoringCheckBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.findUsages.KotlinPropertyFindUsagesOptions;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.KtNamedDeclaration;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;

import javax.swing.*;

public class KotlinFindPropertyUsagesDialog extends JavaFindUsagesDialog<KotlinPropertyFindUsagesOptions> {
    public KotlinFindPropertyUsagesDialog(
            PsiElement element,
            Project project,
            KotlinPropertyFindUsagesOptions findUsagesOptions,
            boolean toShowInNewTab,
            boolean mustOpenInNewTab,
            boolean isSingleFile,
            FindUsagesHandler handler
    ) {
        super(element, project, findUsagesOptions, toShowInNewTab, mustOpenInNewTab, isSingleFile, handler);
    }

    private StateRestoringCheckBox cbReaders;
    private StateRestoringCheckBox cbWriters;
    private StateRestoringCheckBox cbOverrides;
    private StateRestoringCheckBox cbExpected;

    @NotNull
    @Override
    protected KotlinPropertyFindUsagesOptions getFindUsagesOptions() {
        return (KotlinPropertyFindUsagesOptions) myFindUsagesOptions;
    }

    @Override
    public JComponent getPreferredFocusedControl() {
        return myCbToSkipResultsWhenOneUsage;
    }

    @Override
    public void calcFindUsagesOptions(KotlinPropertyFindUsagesOptions options) {
        super.calcFindUsagesOptions(options);

        options.isReadAccess = isSelected(cbReaders);
        options.isWriteAccess = isSelected(cbWriters);
        options.setSearchOverrides(isSelected(cbOverrides));
        options.setSearchExpected(isSelected(cbExpected));
    }

    @Override
    protected JPanel createFindWhatPanel() {
        JPanel findWhatPanel = new JPanel();
        findWhatPanel.setBorder(IdeBorderFactory.createTitledBorder(FindBundle.message("find.what.group"), true));
        findWhatPanel.setLayout(new BoxLayout(findWhatPanel, BoxLayout.Y_AXIS));

        KotlinPropertyFindUsagesOptions options = getFindUsagesOptions();

        cbReaders = addCheckboxToPanel(
                KotlinBundle.message("find.what.property.readers.checkbox"),
                options.isReadAccess,
                findWhatPanel,
                true
        );
        cbWriters = addCheckboxToPanel(
                KotlinBundle.message("find.what.property.writers.checkbox"),
                options.isWriteAccess,
                findWhatPanel,
                true
        );

        return findWhatPanel;
    }

    @Override
    public void configureLabelComponent(@NotNull SimpleColoredComponent coloredComponent) {
        Utils.configureLabelComponent(coloredComponent, (KtNamedDeclaration) getPsiElement());
    }

    @Override
    protected void addUsagesOptions(JPanel optionsPanel) {
        super.addUsagesOptions(optionsPanel);

        KtNamedDeclaration property = (KtNamedDeclaration) getPsiElement();

        boolean isAbstract = property.hasModifier(KtTokens.ABSTRACT_KEYWORD);
        boolean isOpen = property.hasModifier(KtTokens.OPEN_KEYWORD);
        if (isOpen || isAbstract) {
            cbOverrides = addCheckboxToPanel(
                    isAbstract
                    ? KotlinBundle.message("find.what.implementing.properties.checkbox")
                    : KotlinBundle.message("find.what.overriding.properties.checkbox"),
                    FindSettings.getInstance().isSearchOverloadedMethods(),
                    optionsPanel,
                    false
            );
        }
        boolean isActual = PsiUtilsKt.hasActualModifier(property);
        KotlinPropertyFindUsagesOptions options = getFindUsagesOptions();
        if (isActual) {
            cbExpected = addCheckboxToPanel(
                    "Expected properties",
                    options.getSearchExpected(),
                    optionsPanel,
                    false
            );
        }
    }

    @Override
    protected void update() {
        setOKActionEnabled(isSelected(cbReaders) || isSelected(cbWriters));
    }
}
