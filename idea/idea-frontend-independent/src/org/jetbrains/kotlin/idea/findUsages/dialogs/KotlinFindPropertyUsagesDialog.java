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
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.StateRestoringCheckBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinBundleIndependent;
import org.jetbrains.kotlin.idea.findUsages.KotlinPropertyFindUsagesOptions;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
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

    private StateRestoringCheckBox readAccesses;
    private StateRestoringCheckBox writeAccesses;
    private StateRestoringCheckBox overrideUsages;
    private StateRestoringCheckBox expectedUsages;

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

        options.isReadAccess = isSelected(readAccesses);
        options.isWriteAccess = isSelected(writeAccesses);
        options.setSearchOverrides(isSelected(overrideUsages));
        if (expectedUsages != null) {
            options.setSearchExpected(expectedUsages.isSelected());
        }
    }

    @Override
    protected JPanel createFindWhatPanel() {
        JPanel findWhatPanel = new JPanel();
        findWhatPanel.setBorder(IdeBorderFactory.createTitledBorder(FindBundle.message("find.what.group"), true));
        findWhatPanel.setLayout(new BoxLayout(findWhatPanel, BoxLayout.Y_AXIS));

        KotlinPropertyFindUsagesOptions options = getFindUsagesOptions();

        readAccesses = addCheckboxToPanel(
                KotlinBundleIndependent.message("find.declaration.property.readers.checkbox"),
                options.isReadAccess,
                findWhatPanel,
                true
        );
        writeAccesses = addCheckboxToPanel(
                KotlinBundleIndependent.message("find.declaration.property.writers.checkbox"),
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
            overrideUsages = addCheckboxToPanel(
                    isAbstract
                    ? KotlinBundleIndependent.message("find.declaration.implementing.properties.checkbox")
                    : KotlinBundleIndependent.message("find.declaration.overriding.properties.checkbox"),
                    FindSettings.getInstance().isSearchOverloadedMethods(),
                    optionsPanel,
                    false
            );
        }
        boolean isActual = PsiUtilsKt.hasActualModifier(property);
        KotlinPropertyFindUsagesOptions options = getFindUsagesOptions();
        if (isActual) {
            expectedUsages = addCheckboxToPanel(
                    KotlinBundleIndependent.message("find.usages.checkbox.name.expected.properties"),
                    options.getSearchExpected(),
                    optionsPanel,
                    false
            );
        }

        if (isDataClassConstructorProperty(property)) {
            JCheckBox dataClassComponentCheckBox =
                    new JCheckBox(KotlinBundleIndependent.message("find.usages.checkbox.text.fast.data.class.component.search"));
            dataClassComponentCheckBox.setToolTipText(KotlinBundleIndependent.message(
                    "find.usages.tool.tip.text.disable.search.for.data.class.components.and.destruction.declarations.project.wide.setting"));
            Project project = property.getProject();
            dataClassComponentCheckBox.setSelected(getDisableComponentAndDestructionSearch(project));
            optionsPanel.add(dataClassComponentCheckBox);
            dataClassComponentCheckBox.addActionListener(
                    ___ -> setDisableComponentAndDestructionSearch(project, dataClassComponentCheckBox.isSelected())
            );
        }
    }

    @Override
    protected void update() {
        setOKActionEnabled(isSelected(readAccesses) || isSelected(writeAccesses));
    }

    private static final boolean disableComponentAndDestructionSearchDefault = false;
    private static final String optionName = "kotlin.disable.search.component.and.destruction";

    public static boolean getDisableComponentAndDestructionSearch(Project project) {
        return PropertiesComponent.getInstance(project).getBoolean(optionName, disableComponentAndDestructionSearchDefault);
    }

    public static void setDisableComponentAndDestructionSearch(Project project, boolean value) {
        PropertiesComponent.getInstance(project).setValue(optionName, value, disableComponentAndDestructionSearchDefault);
    }

    private static boolean isDataClassConstructorProperty(KtNamedDeclaration declaration) {
        if (declaration instanceof KtParameter) {
            PsiElement parent = declaration.getParent();
            if (parent instanceof KtParameterList) {
                parent = parent.getParent();
                if (parent instanceof KtPrimaryConstructor) {
                    parent = parent.getParent();
                    if (parent instanceof KtClass) {
                        return ((KtClass)parent).isData();
                    }
                }
            }
        }
        return false;
    }
}
