package org.jetbrains.jet.plugin.findUsages.dialogs;

import com.intellij.find.FindBundle;
import com.intellij.find.FindSettings;
import com.intellij.find.findUsages.FindMethodUsagesDialog;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.SimpleColoredComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.findUsages.options.KotlinMethodFindUsagesOptions;
import org.jetbrains.jet.plugin.refactoring.JetRefactoringUtil;

import javax.swing.*;
import java.awt.*;

public class KotlinFindMethodUsagesDialog extends FindMethodUsagesDialog {
    public KotlinFindMethodUsagesDialog(
            PsiMethod method,
            Project project,
            KotlinMethodFindUsagesOptions findUsagesOptions,
            boolean toShowInNewTab,
            boolean mustOpenInNewTab,
            boolean isSingleFile,
            FindUsagesHandler handler
    ) {
        super(method, project, findUsagesOptions, toShowInNewTab, mustOpenInNewTab, isSingleFile, handler);
    }

    @Override
    protected KotlinMethodFindUsagesOptions getFindUsagesOptions() {
        return (KotlinMethodFindUsagesOptions) super.getFindUsagesOptions();
    }

    @Override
    public void configureLabelComponent(@NotNull SimpleColoredComponent coloredComponent) {
        coloredComponent.append(JetRefactoringUtil.formatJavaOrLightMethod((PsiMethod) myPsiElement));
    }

    private static boolean renameCheckbox(@NotNull JPanel panel, @NotNull String srcText, @NotNull String destText) {
        for (Component component : panel.getComponents()) {
            if (component instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) component;
                if (checkBox.getText().equals(srcText)) {
                    checkBox.setText(destText);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected JPanel createFindWhatPanel() {
        JPanel findWhatPanel = super.createFindWhatPanel();
        if (findWhatPanel != null) {
            renameCheckbox(
                    findWhatPanel,
                    FindBundle.message("find.what.implementing.methods.checkbox"),
                    JetBundle.message("find.what.implementing.methods.checkbox")
            );
            renameCheckbox(
                    findWhatPanel,
                    FindBundle.message("find.what.overriding.methods.checkbox"),
                    JetBundle.message("find.what.overriding.methods.checkbox")
            );
        }

        return findWhatPanel;
    }

    @Override
    protected void addUsagesOptions(JPanel optionsPanel) {
        super.addUsagesOptions(optionsPanel);

        if (!renameCheckbox(
                optionsPanel,
                FindBundle.message("find.options.include.overloaded.methods.checkbox"),
                JetBundle.message("find.options.include.overloaded.methods.checkbox")
        )) {
            addCheckboxToPanel(
                    JetBundle.message("find.options.include.overloaded.methods.checkbox"),
                    FindSettings.getInstance().isSearchOverloadedMethods(),
                    optionsPanel,
                    false
            );
        }
    }
}
