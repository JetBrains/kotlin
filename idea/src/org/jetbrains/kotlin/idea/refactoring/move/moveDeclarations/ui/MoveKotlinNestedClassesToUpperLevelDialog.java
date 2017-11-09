/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.JavaProjectRootsUtil;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.NullableComputable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.PackageWrapper;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.move.MoveDialogBase;
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesUtil;
import com.intellij.refactoring.move.moveInner.MoveInnerImpl;
import com.intellij.refactoring.ui.NameSuggestionsField;
import com.intellij.refactoring.ui.PackageNameReferenceEditorCombo;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.refactoring.util.RefactoringMessageUtil;
import com.intellij.refactoring.util.RefactoringUtil;
import com.intellij.ui.EditorTextField;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.idea.KotlinFileType;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.core.CollectingNameValidator;
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester;
import org.jetbrains.kotlin.idea.core.NewDeclarationNameValidator;
import org.jetbrains.kotlin.idea.core.PackageUtilsKt;
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringSettings;
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringUtilKt;
import org.jetbrains.kotlin.idea.refactoring.move.MoveUtilsKt;
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.*;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.types.KotlinType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
        setTitle("Move Nested Classes to Upper Level");
        init();
        packageNameLabel.setLabelFor(packageNameField.getChildComponent());
        classNameLabel.setLabelFor(classNameField);
        parameterNameLabel.setLabelFor(parameterField);
        openInEditorPanel.add(initOpenInEditorCb(), BorderLayout.EAST);
    }

    @Nullable
    private static FqName getTargetPackageFqName(PsiElement targetContainer) {
        if (targetContainer instanceof PsiDirectory) {
            PsiPackage targetPackage = PackageUtilsKt.getPackage((PsiDirectory) targetContainer);
            return targetPackage != null ? new FqName(targetPackage.getQualifiedName()) : null;
        }
        if (targetContainer instanceof KtFile) return ((KtFile) targetContainer).getPackageFqName();
        return null;
    }

    private void createUIComponents() {
        parameterField = new NameSuggestionsField(project);
        packageNameField = new PackageNameReferenceEditorCombo("", project, RECENTS_KEY,
                                                               RefactoringBundle.message("choose.destination.package"));
    }

    @Override
    protected String getMovePropertySuffix() {
        return "Nested Classes to Upper Level";
    }

    @Override
    protected String getHelpId() {
        return HelpID.MOVE_INNER_UPPER;
    }

    @Override
    protected String getCbTitle() {
        return "Open moved member in editor";
    }

    public boolean isSearchInComments() {
        return searchInCommentsCheckBox.isSelected();
    }

    public boolean isSearchInNonJavaFiles() {
        return searchForTextOccurrencesCheckBox.isSelected();
    }

    public String getClassName() {
        return classNameField.getText().trim();
    }

    @Nullable
    public String getParameterName() {
        return parameterField != null ? parameterField.getEnteredName() : null;
    }

    private boolean isThisNeeded() {
        return innerClass instanceof KtClass && MoveUtilsKt.traverseOuterInstanceReferences((KtClass) innerClass, true);
    }

    @Nullable
    private FqName getTargetPackageFqName() {
        return getTargetPackageFqName(targetContainer);
    }

    @NotNull
    private KotlinType getOuterInstanceType() {
        return ((ClassDescriptor) innerClassDescriptor.getContainingDeclaration()).getDefaultType();
    }

    @Override
    protected void init() {
        classNameField.setText(innerClass.getName());
        classNameField.selectAll();

        if (innerClass instanceof KtClass && ((KtClass) innerClass).isInner()) {
            passOuterClassCheckBox.setSelected(true);
            passOuterClassCheckBox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    parameterField.setEnabled(passOuterClassCheckBox.isSelected());
                }
            });
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

        passOuterClassCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                boolean selected = passOuterClassCheckBox.isSelected();
                parameterField.getComponent().setEnabled(selected);
            }
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
                                                      Collections.<KtDeclaration>emptyList())
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

    @Nullable
    private PsiElement getTargetContainer() {
        if (targetContainer instanceof PsiDirectory) {
            PsiDirectory psiDirectory = (PsiDirectory) targetContainer;
            FqName oldPackageFqName = getTargetPackageFqName();
            String targetName = packageNameField.getText();
            if (!Comparing.equal(oldPackageFqName != null ? oldPackageFqName.asString() : null, targetName)) {
                ProjectRootManager projectRootManager = ProjectRootManager.getInstance(project);
                List<VirtualFile> contentSourceRoots = JavaProjectRootsUtil.getSuitableDestinationSourceRoots(project);
                final PackageWrapper newPackage = new PackageWrapper(PsiManager.getInstance(project), targetName);
                final VirtualFile targetSourceRoot;
                if (contentSourceRoots.size() > 1) {
                    PsiDirectory initialDir = null;
                    PsiPackage oldPackage = oldPackageFqName != null
                                            ? JavaPsiFacade.getInstance(project).findPackage(oldPackageFqName.asString())
                                            : null;
                    if (oldPackage != null) {
                        PsiDirectory[] directories = oldPackage.getDirectories();
                        VirtualFile root = projectRootManager.getFileIndex().getContentRootForFile(psiDirectory.getVirtualFile());
                        for (PsiDirectory dir : directories) {
                            if (Comparing.equal(projectRootManager.getFileIndex().getContentRootForFile(dir.getVirtualFile()), root)) {
                                initialDir = dir;
                            }
                        }
                    }
                    VirtualFile sourceRoot = MoveClassesOrPackagesUtil.chooseSourceRoot(newPackage, contentSourceRoots, initialDir);
                    if (sourceRoot == null) return null;
                    targetSourceRoot = sourceRoot;
                }
                else {
                    targetSourceRoot = contentSourceRoots.get(0);
                }
                PsiDirectory dir = RefactoringUtil.findPackageDirectoryInSourceRoot(newPackage, targetSourceRoot);
                if (dir == null) {
                    dir = ApplicationManager.getApplication().runWriteAction(new NullableComputable<PsiDirectory>() {
                        @Override
                        public PsiDirectory compute() {
                            try {
                                return RefactoringUtil.createPackageDirectoryInSourceRoot(newPackage, targetSourceRoot);
                            }
                            catch (IncorrectOperationException e) {
                                return null;
                            }
                        }
                    });
                }
                return dir;
            }

            return targetContainer;
        }

        if (targetContainer instanceof KtFile || targetContainer instanceof KtClassOrObject) return targetContainer;

        return null;
    }

    @Nullable
    private PsiElement getTargetContainerWithValidation() throws ConfigurationException {
        String className = getClassName();
        String parameterName = getParameterName();

        if (className != null && className.isEmpty()) throw new ConfigurationException(RefactoringBundle.message("no.class.name.specified"));
        if (!KotlinNameSuggester.INSTANCE.isIdentifier(className)) throw new ConfigurationException(RefactoringMessageUtil.getIncorrectIdentifierMessage(className));

        if (passOuterClassCheckBox.isSelected()) {
            if (parameterName != null && parameterName.isEmpty()) throw new ConfigurationException(RefactoringBundle.message("no.parameter.name.specified"));
            if (!KotlinNameSuggester.INSTANCE.isIdentifier(parameterName)) throw new ConfigurationException(RefactoringMessageUtil.getIncorrectIdentifierMessage(parameterName));
        }

        PsiElement targetContainer = getTargetContainer();

        if (targetContainer instanceof KtClassOrObject) {
            KtClassOrObject targetClass = (KtClassOrObject) targetContainer;
            for (KtDeclaration member : targetClass.getDeclarations()) {
                if (member instanceof KtClassOrObject && className != null && className.equals(member.getName())) {
                    throw new ConfigurationException(RefactoringBundle.message("inner.class.exists", className, targetClass.getName()));
                }
            }
        }

        if (targetContainer instanceof PsiDirectory || targetContainer instanceof KtFile) {
            FqName targetPackageFqName = getTargetPackageFqName();
            if (targetPackageFqName == null) throw new ConfigurationException("No package corresponds to this directory");

            //noinspection ConstantConditions
            ClassifierDescriptor existingClass = DescriptorUtils
                    .getContainingModule(innerClassDescriptor)
                    .getPackage(targetPackageFqName)
                    .getMemberScope()
                    .getContributedClassifier(Name.identifier(className), NoLookupLocation.FROM_IDE);
            if (existingClass != null) throw new ConfigurationException("Class " + className + " already exists in package " +  targetPackageFqName);

            PsiDirectory targetDir = targetContainer instanceof PsiDirectory
                                     ? (PsiDirectory) targetContainer
                                     : targetContainer.getContainingFile().getContainingDirectory();
            String message = RefactoringMessageUtil.checkCanCreateFile(targetDir, className + ".kt");
            if (message != null) throw new ConfigurationException(message);
        }

        return targetContainer;
    }

    @Override
    protected void doAction() {
        PsiElement target;
        try {
            target = getTargetContainerWithValidation();
            if (target == null) return;
        }
        catch (ConfigurationException e) {
            CommonRefactoringUtil.showErrorMessage(MoveInnerImpl.REFACTORING_NAME, e.getMessage(), HelpID.MOVE_INNER_UPPER, project);
            return;
        }

        KotlinRefactoringSettings settings = KotlinRefactoringSettings.getInstance();
        settings.MOVE_TO_UPPER_LEVEL_SEARCH_FOR_TEXT = searchForTextOccurrencesCheckBox.isSelected();
        settings.MOVE_TO_UPPER_LEVEL_SEARCH_IN_COMMENTS = searchInCommentsCheckBox.isSelected();

        KotlinMoveTarget moveTarget;
        if (target instanceof PsiDirectory) {
            final PsiDirectory targetDir = (PsiDirectory) target;

            final FqName targetPackageFqName = getTargetPackageFqName(target);
            if (targetPackageFqName == null) return;

            String innerClassName = innerClass.getName();
            if (innerClassName == null) return;
            final String targetFileName = KotlinNameSuggester.INSTANCE.suggestNameByName(
                    innerClassName,
                    new Function1<String, Boolean>() {
                        @Override
                        public Boolean invoke(String s) {
                            return targetDir.findFile(s + "." + KotlinFileType.EXTENSION) == null;
                        }
                    }
            ) + "." + KotlinFileType.EXTENSION;
            moveTarget = new KotlinMoveTargetForDeferredFile(
                    targetPackageFqName,
                    targetDir,
                    null,
                    new Function1<KtFile, KtFile>() {
                        @Override
                        public KtFile invoke(@NotNull KtFile originalFile) {
                            return KotlinRefactoringUtilKt.createKotlinFile(targetFileName, targetDir, targetPackageFqName.asString());
                        }
                    }
            );
        }
        else {
            //noinspection ConstantConditions
            moveTarget = new KotlinMoveTargetForExistingElement((KtElement) target);
        }

        String outerInstanceParameterName = passOuterClassCheckBox.isSelected() ? getParameterName() : null;
        String newClassName = getClassName();
        MoveDeclarationsDelegate delegate = new MoveDeclarationsDelegate.NestedClass(newClassName, outerInstanceParameterName);
        MoveDeclarationsDescriptor moveDescriptor = new MoveDeclarationsDescriptor(
                project,
                CollectionsKt.listOf(innerClass),
                moveTarget,
                delegate,
                isSearchInComments(),
                isSearchInNonJavaFiles(),
                false,
                false,
                null,
                isOpenInEditor()
        );
        saveOpenInEditorOption();

        invokeRefactoring(new MoveKotlinDeclarationsProcessor(moveDescriptor, Mover.Default.INSTANCE));
    }
}