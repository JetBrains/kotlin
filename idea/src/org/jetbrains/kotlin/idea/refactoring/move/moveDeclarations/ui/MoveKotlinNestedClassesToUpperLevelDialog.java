/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.PackageWrapper;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.move.MoveDialogBase;
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesUtil;
import com.intellij.refactoring.ui.NameSuggestionsField;
import com.intellij.refactoring.ui.PackageNameReferenceEditorCombo;
import com.intellij.ui.EditorTextField;
import com.intellij.util.ArrayUtil;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.core.CollectingNameValidator;
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester;
import org.jetbrains.kotlin.idea.core.NewDeclarationNameValidator;
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringSettings;
import org.jetbrains.kotlin.idea.refactoring.move.MoveUtilsKt;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassBody;
import org.jetbrains.kotlin.psi.KtClassOrObject;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.types.KotlinType;

import javax.swing.*;
import java.awt.*;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

public class MoveKotlinNestedClassesToUpperLevelDialog extends MoveDialogBase {
    @NonNls private static final String RECENTS_KEY = MoveKotlinNestedClassesToUpperLevelDialog.class.getName() + ".RECENTS_KEY";

    private final Project project;
    private final KtClassOrObject innerClass;
    private final ClassDescriptor innerClassDescriptor;

    private final PsiElement targetContainer;
    private EditorTextField classNameField;
    private NameSuggestionsField parameterField;
    private JCheckBox passOuterClassCheckBox;
    private JPanel panel;
    private JCheckBox searchInCommentsCheckBox;
    private JCheckBox searchForTextOccurrencesCheckBox;
    private PackageNameReferenceEditorCombo packageNameField;
    private JLabel packageNameLabel;
    private JLabel classNameLabel;
    private JLabel parameterNameLabel;
    private JPanel openInEditorPanel;

    public MoveKotlinNestedClassesToUpperLevelDialog(
            @NotNull Project project,
            @NotNull KtClassOrObject innerClass,
            @NotNull PsiElement targetContainer
    ) {
        super(project, true);
        this.project = project;
        this.innerClass = innerClass;
        this.targetContainer = targetContainer;
        this.innerClassDescriptor = (ClassDescriptor) ResolutionUtils.unsafeResolveToDescriptor(innerClass, BodyResolveMode.FULL);
        setTitle(KotlinBundle.message("title.move.nested.classes.to.upper.level"));
        init();
        packageNameLabel.setLabelFor(packageNameField.getChildComponent());
        classNameLabel.setLabelFor(classNameField);
        parameterNameLabel.setLabelFor(parameterField);
        openInEditorPanel.add(initOpenInEditorCb(), BorderLayout.EAST);
    }

    private void createUIComponents() {
        parameterField = new NameSuggestionsField(project);
        packageNameField = new PackageNameReferenceEditorCombo("", project, RECENTS_KEY,
                                                               RefactoringBundle.message("choose.destination.package"));
    }

    @Override
    protected String getMovePropertySuffix() {
        return KotlinBundle.message("text.nested.classes.to.upper.level");
    }

    @Override
    protected String getHelpId() {
        return HelpID.MOVE_INNER_UPPER;
    }

    @Override
    protected String getCbTitle() {
        return KotlinBundle.message("checkbox.text.open.moved.files.in.editor");
    }

    public String getClassName() {
        return classNameField.getText().trim();
    }

    @Nullable
    public String getParameterName() {
        return parameterField != null ? parameterField.getEnteredName() : null;
    }

    private boolean isThisNeeded() {
        return innerClass instanceof KtClass && MoveUtilsKt.traverseOuterInstanceReferences(innerClass, true);
    }

    @Nullable
    private FqName getTargetPackageFqName() {
        return MoveUtilsKt.getTargetPackageFqName(targetContainer);
    }

    @NotNull
    private KotlinType getOuterInstanceType() {
        return ((ClassDescriptor) innerClassDescriptor.getContainingDeclaration()).getDefaultType();
    }

    private BitSet initializedCheckBoxesState;
    private BitSet getCheckboxesState(boolean applyDefaults) {

        BitSet state = new BitSet(3);

        state.set(0, !applyDefaults && searchInCommentsCheckBox.isSelected()); //searchInCommentsCheckBox default is false
        state.set(1, !applyDefaults && searchForTextOccurrencesCheckBox.isSelected()); //searchForTextOccurrencesCheckBox default is false
        state.set(2, passOuterClassCheckBox.isSelected());

        return state;
    }


    @Override
    protected void init() {
        classNameField.setText(innerClass.getName());
        classNameField.selectAll();

        if (innerClass instanceof KtClass && ((KtClass) innerClass).isInner()) {
            passOuterClassCheckBox.setSelected(true);
            passOuterClassCheckBox.addItemListener(e -> parameterField.setEnabled(passOuterClassCheckBox.isSelected()));
        }
        else {
            passOuterClassCheckBox.setSelected(false);
            passOuterClassCheckBox.setEnabled(false);
            parameterField.setEnabled(false);
        }

        if (passOuterClassCheckBox.isEnabled()) {
            boolean thisNeeded = isThisNeeded();
            passOuterClassCheckBox.setSelected(thisNeeded);
            parameterField.setEnabled(thisNeeded);
        }

        passOuterClassCheckBox.addItemListener(e -> {
            boolean selected = passOuterClassCheckBox.isSelected();
            parameterField.getComponent().setEnabled(selected);
        });

        if (!(targetContainer instanceof PsiDirectory)) {
            packageNameField.setVisible(false);
            packageNameLabel.setVisible(false);
        }

        if (innerClass instanceof KtClass && ((KtClass) innerClass).isInner()) {
            KtClassBody innerClassBody = innerClass.getBody();
            Function1<String, Boolean> validator =
                    innerClassBody != null
                    ? new NewDeclarationNameValidator(innerClassBody, (PsiElement) null,
                                                      NewDeclarationNameValidator.Target.VARIABLES,
                                                      Collections.emptyList())
                    : new CollectingNameValidator();
            List<String> suggestions = KotlinNameSuggester.INSTANCE.suggestNamesByType(getOuterInstanceType(), validator, "outer");
            parameterField.setSuggestions(ArrayUtil.toStringArray(suggestions));
        }
        else {
            parameterField.getComponent().setEnabled(false);
        }

        FqName packageFqName = getTargetPackageFqName();
        if (packageFqName != null) {
            packageNameField.prependItem(packageFqName.asString());
        }

        KotlinRefactoringSettings settings = KotlinRefactoringSettings.getInstance();
        searchForTextOccurrencesCheckBox.setSelected(settings.MOVE_TO_UPPER_LEVEL_SEARCH_FOR_TEXT);
        searchInCommentsCheckBox.setSelected(settings.MOVE_TO_UPPER_LEVEL_SEARCH_IN_COMMENTS);

        super.init();

        initializedCheckBoxesState = getCheckboxesState(true);
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return classNameField;
    }

    @Override
    protected String getDimensionServiceKey() {
        return "#com.intellij.refactoring.move.moveInner.MoveInnerDialog";
    }

    @Override
    protected JComponent createNorthPanel() {
        return panel;
    }

    @Override
    protected JComponent createCenterPanel() {
        return null;
    }


    private static class MoveKotlinNestedClassesToUpperLevelModelWithUIChooser extends MoveKotlinNestedClassesToUpperLevelModel {
        public MoveKotlinNestedClassesToUpperLevelModelWithUIChooser(
                @NotNull Project project,
                @NotNull KtClassOrObject innerClass,
                @NotNull PsiElement target,
                @Nullable String parameter,
                @NotNull String className,
                boolean passOuterClass,
                boolean searchInComments,
                boolean isSearchInNonJavaFiles,
                @NotNull String packageName,
                boolean isOpenInEditor
        ) {
            super(project, innerClass, target, parameter, className, passOuterClass, searchInComments, isSearchInNonJavaFiles, packageName,
                  isOpenInEditor);
        }

        @Nullable
        @Override
        protected VirtualFile chooseSourceRoot(
                @NotNull PackageWrapper newPackage,
                @NotNull List<? extends VirtualFile> contentSourceRoots,
                @Nullable PsiDirectory initialDir
        ) {
            return MoveClassesOrPackagesUtil.chooseSourceRoot(newPackage, contentSourceRoots, initialDir);
        }
    }

    private Model getModel() {
        return new MoveKotlinNestedClassesToUpperLevelModelWithUIChooser(
                project,
                innerClass,
                targetContainer,
                getParameterName(),
                getClassName(),
                passOuterClassCheckBox.isSelected(),
                searchInCommentsCheckBox.isSelected(),
                searchForTextOccurrencesCheckBox.isSelected(),
                packageNameField.getText(),
                isOpenInEditor()
        );
    }

    @Override
    protected void doAction() {

        ModelResultWithFUSData modelResult;
        try {
            modelResult = getModel().computeModelResult();
        }
        catch (ConfigurationException e) {
            setErrorText(e.getMessage());
            return;
        }

        KotlinRefactoringSettings settings = KotlinRefactoringSettings.getInstance();
        settings.MOVE_TO_UPPER_LEVEL_SEARCH_FOR_TEXT = searchForTextOccurrencesCheckBox.isSelected();
        settings.MOVE_TO_UPPER_LEVEL_SEARCH_IN_COMMENTS = searchInCommentsCheckBox.isSelected();

        saveOpenInEditorOption();

        MoveUtilsKt.logFusForMoveRefactoring(
                modelResult.getElementsCount(),
                modelResult.getEntityToMove(),
                modelResult.getDestination(),
                getCheckboxesState(false).equals(initializedCheckBoxesState),
                () -> invokeRefactoring(modelResult.getProcessor())
        );
    }
}