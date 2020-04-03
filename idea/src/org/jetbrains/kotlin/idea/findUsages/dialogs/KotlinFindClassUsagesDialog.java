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
import com.intellij.find.findUsages.FindClassUsagesDialog;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.find.findUsages.JavaClassFindUsagesOptions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiModifier;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.StateRestoringCheckBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.LightClassUtilsKt;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.core.PsiModificationUtilsKt;
import org.jetbrains.kotlin.idea.findUsages.KotlinClassFindUsagesOptions;
import org.jetbrains.kotlin.idea.refactoring.RenderingUtilsKt;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;

import javax.swing.*;

import static org.jetbrains.kotlin.asJava.LightClassUtilsKt.toLightClass;

public class KotlinFindClassUsagesDialog extends FindClassUsagesDialog {
    private StateRestoringCheckBox constructorUsages;
    private StateRestoringCheckBox derivedClasses;
    private StateRestoringCheckBox derivedTraits;
    private StateRestoringCheckBox expectedUsages;

    public KotlinFindClassUsagesDialog(
            KtClassOrObject classOrObject,
            Project project,
            FindUsagesOptions findUsagesOptions,
            boolean toShowInNewTab,
            boolean mustOpenInNewTab,
            boolean isSingleFile,
            FindUsagesHandler handler
    ) {
        super(getRepresentingPsiClass(classOrObject), project, findUsagesOptions, toShowInNewTab, mustOpenInNewTab, isSingleFile, handler);
    }

    private static final Key<KtClassOrObject> ORIGINAL_CLASS = Key.create("ORIGINAL_CLASS");

    @NotNull
    private static PsiClass getRepresentingPsiClass(@NotNull KtClassOrObject classOrObject) {
        PsiClass lightClass = toLightClass(classOrObject);
        if (lightClass != null) return lightClass;

        // TODO: Remove this code when light classes are generated for builtins
        PsiElementFactory factory = PsiElementFactory.SERVICE.getInstance(classOrObject.getProject());

        String name = classOrObject.getName();
        if (name == null || name.isEmpty()) {
            name = KotlinBundle.message("find.usages.class.name.anonymous");
        }

        PsiClass javaClass;
        if (classOrObject instanceof KtClass) {
            KtClass klass = (KtClass) classOrObject;
            javaClass = !klass.isInterface()
                        ? factory.createClass(name)
                        : klass.isAnnotation()
                          ? factory.createAnnotationType(name)
                          : factory.createInterface(name);
        }
        else {
            javaClass = factory.createClass(name);
        }

        //noinspection ConstantConditions
        javaClass.getModifierList().setModifierProperty(
                PsiModifier.FINAL,
                !(classOrObject instanceof KtClass && PsiModificationUtilsKt.isInheritable((KtClass) classOrObject))
        );

        javaClass.putUserData(ORIGINAL_CLASS, classOrObject);

        return javaClass;
    }

    @Override
    protected JPanel createFindWhatPanel() {
        JPanel findWhatPanel = super.createFindWhatPanel();
        assert findWhatPanel != null;

        Utils.renameCheckbox(
                findWhatPanel,
                FindBundle.message("find.what.methods.usages.checkbox"),
                KotlinBundle.message("find.declaration.functions.usages.checkbox")
        );
        Utils.renameCheckbox(
                findWhatPanel,
                FindBundle.message("find.what.fields.usages.checkbox"),
                KotlinBundle.message("find.declaration.properties.usages.checkbox")
        );
        Utils.removeCheckbox(findWhatPanel, FindBundle.message("find.what.implementing.classes.checkbox"));
        Utils.removeCheckbox(findWhatPanel, FindBundle.message("find.what.derived.interfaces.checkbox"));
        Utils.removeCheckbox(findWhatPanel, FindBundle.message("find.what.derived.classes.checkbox"));

        derivedClasses = addCheckboxToPanel(
                KotlinBundle.message("find.declaration.derived.classes.checkbox"),
                getFindUsagesOptions().isDerivedClasses,
                findWhatPanel,
                true
        );
        derivedTraits = addCheckboxToPanel(
                KotlinBundle.message("find.declaration.derived.interfaces.checkbox"),
                getFindUsagesOptions().isDerivedInterfaces,
                findWhatPanel,
                true
        );
        constructorUsages = addCheckboxToPanel(
                KotlinBundle.message("find.declaration.constructor.usages.checkbox"),
                getFindUsagesOptions().getSearchConstructorUsages(),
                findWhatPanel,
                true
        );
        return findWhatPanel;
    }

    @NotNull
    @Override
    protected KotlinClassFindUsagesOptions getFindUsagesOptions() {
        return (KotlinClassFindUsagesOptions) super.getFindUsagesOptions();
    }

    @Nullable
    private KtClassOrObject getOriginalClass() {
        PsiElement klass = LightClassUtilsKt.getUnwrapped(getPsiElement());
        //noinspection ConstantConditions
        return klass instanceof KtClassOrObject
               ? (KtClassOrObject) klass
               : klass.getUserData(ORIGINAL_CLASS);
    }

    @Override
    protected void addUsagesOptions(JPanel optionsPanel) {
        super.addUsagesOptions(optionsPanel);

        KtClassOrObject klass = getOriginalClass();
        boolean isActual = klass != null && PsiUtilsKt.hasActualModifier(klass);
        KotlinClassFindUsagesOptions options = getFindUsagesOptions();
        if (isActual) {
            expectedUsages = addCheckboxToPanel(
                    KotlinBundle.message("find.usages.checkbox.name.expected.classes"),
                    options.getSearchExpected(),
                    optionsPanel,
                    false
            );
        }
    }

    @Override
    public void configureLabelComponent(@NotNull SimpleColoredComponent coloredComponent) {
        KtClassOrObject originalClass = getOriginalClass();
        if (originalClass != null) {
            coloredComponent.append(RenderingUtilsKt.formatClass(originalClass));
        }
    }

    @Override
    protected void update() {
        super.update();
        if (!isOKActionEnabled() && (constructorUsages.isSelected() || derivedTraits.isSelected() || derivedClasses.isSelected())) {
            setOKActionEnabled(true);
        }
    }

    @Override
    public void calcFindUsagesOptions(JavaClassFindUsagesOptions options) {
        super.calcFindUsagesOptions(options);

        KotlinClassFindUsagesOptions kotlinOptions = (KotlinClassFindUsagesOptions) options;
        kotlinOptions.setSearchConstructorUsages(constructorUsages.isSelected());
        kotlinOptions.isDerivedClasses = derivedClasses.isSelected();
        kotlinOptions.isDerivedInterfaces = derivedTraits.isSelected();
        if (expectedUsages != null) {
            kotlinOptions.setSearchExpected(expectedUsages.isSelected());
        }
    }
}
