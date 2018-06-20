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
import com.intellij.find.findUsages.FindMethodUsagesDialog;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.JavaMethodFindUsagesOptions;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.StateRestoringCheckBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.asJava.LightClassUtilsKt;
import org.jetbrains.kotlin.asJava.elements.KtLightMethod;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.findUsages.KotlinFunctionFindUsagesOptions;
import org.jetbrains.kotlin.idea.refactoring.RenderingUtilsKt;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtNamedDeclaration;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;

import javax.swing.*;

public class KotlinFindFunctionUsagesDialog extends FindMethodUsagesDialog {
    private StateRestoringCheckBox expectedUsages;

    public KotlinFindFunctionUsagesDialog(
            PsiMethod method,
            Project project,
            KotlinFunctionFindUsagesOptions findUsagesOptions,
            boolean toShowInNewTab,
            boolean mustOpenInNewTab,
            boolean isSingleFile,
            FindUsagesHandler handler
    ) {
        super(method, project, findUsagesOptions, toShowInNewTab, mustOpenInNewTab, isSingleFile, handler);
    }

    @NotNull
    @Override
    protected KotlinFunctionFindUsagesOptions getFindUsagesOptions() {
        return (KotlinFunctionFindUsagesOptions) myFindUsagesOptions;
    }

    @Override
    public void configureLabelComponent(@NotNull SimpleColoredComponent coloredComponent) {
        coloredComponent.append(RenderingUtilsKt.formatJavaOrLightMethod((PsiMethod) myPsiElement));
    }

    @Override
    protected JPanel createFindWhatPanel() {
        JPanel findWhatPanel = super.createFindWhatPanel();

        if (findWhatPanel != null) {
            Utils.renameCheckbox(
                    findWhatPanel,
                    FindBundle.message("find.what.implementing.methods.checkbox"),
                    KotlinBundle.message("find.what.implementing.methods.checkbox")
            );
            Utils.renameCheckbox(
                    findWhatPanel,
                    FindBundle.message("find.what.overriding.methods.checkbox"),
                    KotlinBundle.message("find.what.overriding.methods.checkbox")
            );
        }

        return findWhatPanel;
    }

    @Override
    protected void addUsagesOptions(JPanel optionsPanel) {
        super.addUsagesOptions(optionsPanel);

        if (!Utils.renameCheckbox(
                optionsPanel,
                FindBundle.message("find.options.include.overloaded.methods.checkbox"),
                KotlinBundle.message("find.options.include.overloaded.methods.checkbox")
        )) {
            addCheckboxToPanel(
                    KotlinBundle.message("find.options.include.overloaded.methods.checkbox"),
                    FindSettings.getInstance().isSearchOverloadedMethods(),
                    optionsPanel,
                    false
            );
        }
        PsiElement element = LightClassUtilsKt.getUnwrapped(getPsiElement());
        //noinspection ConstantConditions
        KtDeclaration function = element instanceof KtNamedDeclaration
                                 ? (KtNamedDeclaration) element
                                 : ((KtLightMethod) element).getKotlinOrigin();

        boolean isActual = function != null && PsiUtilsKt.hasActualModifier(function);
        KotlinFunctionFindUsagesOptions options = getFindUsagesOptions();
        if (isActual) {
            expectedUsages = addCheckboxToPanel(
                    "Expected functions",
                    options.getSearchExpected(),
                    optionsPanel,
                    false
            );
        }
    }

    @Override
    public void calcFindUsagesOptions(JavaMethodFindUsagesOptions options) {
        super.calcFindUsagesOptions(options);

        KotlinFunctionFindUsagesOptions kotlinOptions = (KotlinFunctionFindUsagesOptions) options;
        kotlinOptions.setSearchExpected(isSelected(expectedUsages));
    }
}
