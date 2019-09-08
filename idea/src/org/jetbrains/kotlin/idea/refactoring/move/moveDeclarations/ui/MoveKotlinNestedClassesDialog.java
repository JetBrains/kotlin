/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui;

import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeJavaClassChooserDialog;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.classMembers.AbstractMemberInfoModel;
import com.intellij.refactoring.classMembers.MemberInfoChange;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.move.MoveHandler;
import com.intellij.refactoring.ui.RefactoringDialog;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.LightClassUtilsKt;
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration;
import org.jetbrains.kotlin.idea.completion.CompletionUtilsKt;
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject;
import org.jetbrains.kotlin.idea.core.completion.PackageLookupObject;
import org.jetbrains.kotlin.idea.projectView.KtClassOrObjectTreeNode;
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringUtilKt;
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberInfo;
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberSelectionPanel;
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberSelectionTable;
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.*;
import org.jetbrains.kotlin.idea.refactoring.ui.KotlinTypeReferenceEditorComboWithBrowseButton;
import org.jetbrains.kotlin.psi.*;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Objects;

public class MoveKotlinNestedClassesDialog extends RefactoringDialog {
    private static final String RECENTS_KEY = MoveKotlinNestedClassesDialog.class.getName() + ".RECENTS_KEY";
    private final KtClassOrObject originalClass;
    private final MoveCallback moveCallback;
    private JPanel mainPanel;
    private JTextField originalClassField;
    private JPanel membersInfoPanel;
    private KotlinTypeReferenceEditorComboWithBrowseButton targetClassChooser;
    private JCheckBox openInEditorCheckBox;
    private JPanel targetClassChooserPanel;
    private KotlinMemberSelectionTable memberTable;
    private PsiElement targetClass;

    public MoveKotlinNestedClassesDialog(
            @NotNull Project project,
            @NotNull List<KtClassOrObject> elementsToMove,
            @NotNull KtClassOrObject originalClass,
            @NotNull KtClassOrObject targetClass,
            @Nullable MoveCallback moveCallback
    ) {
        super(project, true);

        this.originalClass = originalClass;
        this.targetClass = targetClass;
        this.moveCallback = moveCallback;

        init();

        setTitle(MoveHandler.REFACTORING_NAME);

        initClassChooser(targetClass);

        initMemberInfo(elementsToMove);

        validateButtons();

        myHelpAction.setEnabled(false);
    }

    private void initClassChooser(@NotNull KtClassOrObject initialTargetClass) {
        //noinspection ConstantConditions
        originalClassField.setText(originalClass.getFqName().asString());

        //noinspection ConstantConditions
        targetClassChooser = new KotlinTypeReferenceEditorComboWithBrowseButton(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        TreeClassChooser chooser = new TreeJavaClassChooserDialog(
                                RefactoringBundle.message("choose.destination.class"),
                                myProject,
                                GlobalSearchScope.projectScope(myProject),
                                aClass -> {
                                    if (!(aClass instanceof KtLightClassForSourceDeclaration)) return false;
                                    KtClassOrObject classOrObject = ((KtLightClassForSourceDeclaration) aClass).getKotlinOrigin();

                                    if (classOrObject instanceof KtObjectDeclaration) {
                                        return !((KtObjectDeclaration) classOrObject).isObjectLiteral();
                                    }

                                    if (classOrObject instanceof KtClass) {
                                        KtClass ktClass = (KtClass) classOrObject;
                                        return !(ktClass.isInner() || ktClass.isAnnotation());
                                    }

                                    return false;
                                },
                                null,
                                null,
                                true
                        ) {
                            @Nullable
                            @Override
                            protected PsiClass getSelectedFromTreeUserObject(DefaultMutableTreeNode node) {
                                PsiClass psiClass = super.getSelectedFromTreeUserObject(node);
                                if (psiClass != null) return psiClass;

                                Object userObject = node.getUserObject();
                                if (!(userObject instanceof KtClassOrObjectTreeNode)) return null;
                                return LightClassUtilsKt.toLightClass(((KtClassOrObjectTreeNode) userObject).getValue());
                            }
                        };
                        chooser.selectDirectory(
                                (targetClass != null ? targetClass : originalClass).getContainingFile().getContainingDirectory());
                        chooser.showDialog();

                        PsiClass aClass = chooser.getSelected();
                        if (aClass instanceof KtLightClassForSourceDeclaration) {
                            targetClass = ((KtLightClassForSourceDeclaration) aClass).getKotlinOrigin();
                            targetClassChooser.setText(Objects.requireNonNull(aClass.getQualifiedName()));
                        }
                        else {
                            targetClass = aClass;
                        }
                    }
                },
                initialTargetClass.getFqName().asString(),
                originalClass,
                RECENTS_KEY);
        KtTypeCodeFragment codeFragment = targetClassChooser.getCodeFragment();
        if (codeFragment != null) {
            CompletionUtilsKt.setExtraCompletionFilter(
                    codeFragment,
                    lookupElement -> {
                        Object lookupObject = lookupElement.getObject();
                        if (!(lookupObject instanceof DeclarationLookupObject)) return false;
                        PsiElement psiElement = ((DeclarationLookupObject) lookupObject).getPsiElement();
                        if (lookupObject instanceof PackageLookupObject) return true;
                        return (psiElement instanceof KtClassOrObject) && KotlinRefactoringUtilKt.canRefactor(psiElement);
                    }
            );
        }
        targetClassChooser.getChildComponent().getDocument().addDocumentListener(
                new DocumentListener() {
                    @Override
                    public void documentChanged(@NotNull DocumentEvent e) {
                        PsiClass aClass = JavaPsiFacade
                                .getInstance(myProject)
                                .findClass(targetClassChooser.getText(), GlobalSearchScope.projectScope(myProject));
                        targetClass = aClass instanceof KtLightClassForSourceDeclaration
                                      ? ((KtLightClassForSourceDeclaration) aClass).getKotlinOrigin()
                                      : aClass;
                        validateButtons();
                    }
                }
        );
        targetClassChooserPanel.add(targetClassChooser);
    }

    private void initMemberInfo(@NotNull List<KtClassOrObject> elementsToMove) {
        List<KotlinMemberInfo> memberInfos = CollectionsKt.mapNotNull(
                originalClass.getDeclarations(),
                declaration -> {
                    if (!(declaration instanceof KtClassOrObject)) return null;
                    KtClassOrObject classOrObject = (KtClassOrObject) declaration;

                    if (classOrObject instanceof KtClass && ((KtClass) classOrObject).isInner()) return null;
                    if (classOrObject instanceof KtObjectDeclaration && ((KtObjectDeclaration) classOrObject).isCompanion()) {
                        return null;
                    }

                    KotlinMemberInfo memberInfo = new KotlinMemberInfo(classOrObject, false);
                    memberInfo.setChecked(elementsToMove.contains(declaration));
                    return memberInfo;
                }
        );
        KotlinMemberSelectionPanel selectionPanel = new KotlinMemberSelectionPanel(getTitle(), memberInfos, null);
        memberTable = selectionPanel.getTable();
        MemberInfoModelImpl memberInfoModel = new MemberInfoModelImpl();
        memberInfoModel.memberInfoChanged(new MemberInfoChange<>(memberInfos));
        selectionPanel.getTable().setMemberInfoModel(memberInfoModel);
        selectionPanel.getTable().addMemberInfoChangeListener(memberInfoModel);
        membersInfoPanel.add(selectionPanel, BorderLayout.CENTER);
    }

    private List<KtClassOrObject> getSelectedElementsToMove() {
        return CollectionsKt.map(
                memberTable.getSelectedMemberInfos(),
                info -> (KtClassOrObject) info.getMember()
        );
    }

    @Override
    protected JComponent createCenterPanel() {
        return mainPanel;
    }

    @Override
    protected String getDimensionServiceKey() {
        return "#" + getClass().getName();
    }

    private Model<MoveKotlinDeclarationsProcessor> getModel() {
        return new MoveKotlinNestedClassesModel(
                myProject,
                openInEditorCheckBox.isSelected(),
                getSelectedElementsToMove(),
                originalClass,
                targetClass,
                moveCallback);
    }

    @Override
    protected void doAction() {

        MoveKotlinDeclarationsProcessor processor;
        try {
            processor = getModel().computeModelResult();
        }
        catch (ConfigurationException e) {
            setErrorText(e.getMessage());
            return;
        }

        invokeRefactoring(processor);
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return targetClassChooser.getChildComponent();
    }

    private static class MemberInfoModelImpl extends AbstractMemberInfoModel<KtNamedDeclaration, KotlinMemberInfo> { }
}
