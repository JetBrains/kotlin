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

import com.intellij.ide.util.ClassFilter;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeJavaClassChooserDialog;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaCodeFragment;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.classMembers.AbstractMemberInfoModel;
import com.intellij.refactoring.classMembers.MemberInfoChange;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.move.MoveHandler;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.ui.ReferenceEditorComboWithBrowseButton;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.KtLightClassForExplicitDeclaration;
import org.jetbrains.kotlin.asJava.LightClassUtil;
import org.jetbrains.kotlin.asJava.LightClassUtilsKt;
import org.jetbrains.kotlin.idea.projectView.KtClassOrObjectTreeNode;
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberInfo;
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberSelectionPanel;
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberSelectionTable;
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.*;
import org.jetbrains.kotlin.psi.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class MoveKotlinNestedClassesDialog extends RefactoringDialog {
    private static final String RECENTS_KEY = MoveKotlinNestedClassesDialog.class.getName() + ".RECENTS_KEY";

    private static class MemberInfoModelImpl extends AbstractMemberInfoModel<KtNamedDeclaration, KotlinMemberInfo> {

    }

    private JPanel mainPanel;
    private JTextField originalClassField;
    private JPanel membersInfoPanel;
    private ReferenceEditorComboWithBrowseButton targetClassChooser;
    private JCheckBox openInEditorCheckBox;
    private JPanel targetClassChooserPanel;
    private KotlinMemberSelectionTable memberTable;

    private final KtClassOrObject originalClass;
    private KtClassOrObject targetClass;
    private final MoveCallback moveCallback;

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
    }

    private void initClassChooser(@NotNull KtClassOrObject initialTargetClass) {
        //noinspection ConstantConditions
        originalClassField.setText(originalClass.getFqName().asString());

        //noinspection ConstantConditions
        targetClassChooser = new ReferenceEditorComboWithBrowseButton(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        TreeClassChooser chooser = new TreeJavaClassChooserDialog(
                                RefactoringBundle.message("choose.destination.class"),
                                myProject,
                                GlobalSearchScope.projectScope(myProject),
                                new ClassFilter() {
                                    @Override
                                    public boolean isAccepted(PsiClass aClass) {
                                        if (!(aClass instanceof KtLightClassForExplicitDeclaration)) return false;
                                        KtClassOrObject classOrObject = ((KtLightClassForExplicitDeclaration) aClass).getOrigin();

                                        if (classOrObject.isTopLevel()) return true;

                                        if (classOrObject instanceof KtObjectDeclaration) {
                                            return !((KtObjectDeclaration) classOrObject).isObjectLiteral();
                                        }

                                        if (classOrObject instanceof KtClass) {
                                            return !((KtClass) classOrObject).isInner();
                                        }

                                        return false;
                                    }
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
                        chooser.selectDirectory((targetClass != null ? targetClass : originalClass).getContainingFile().getContainingDirectory());
                        chooser.showDialog();

                        PsiClass aClass = chooser.getSelected();
                        if (aClass instanceof KtLightClassForExplicitDeclaration) {
                            targetClass = ((KtLightClassForExplicitDeclaration) aClass).getOrigin();
                            targetClassChooser.setText(aClass.getQualifiedName());
                        }
                    }
                },
                initialTargetClass.getFqName().asString(),
                myProject,
                true,
                JavaCodeFragment.VisibilityChecker.PROJECT_SCOPE_VISIBLE,
                RECENTS_KEY);
        targetClassChooser.getChildComponent().getDocument().addDocumentListener(
                new DocumentAdapter() {
                    @Override
                    public void documentChanged(DocumentEvent e) {
                        PsiClass aClass = JavaPsiFacade
                                .getInstance(myProject)
                                .findClass(targetClassChooser.getText(), GlobalSearchScope.projectScope(myProject));
                        targetClass = aClass instanceof KtLightClassForExplicitDeclaration
                                      ? ((KtLightClassForExplicitDeclaration) aClass).getOrigin()
                                      : null;
                        validateButtons();
                    }
                }
        );
        targetClassChooserPanel.add(targetClassChooser);
    }

    private void initMemberInfo(@NotNull final List<KtClassOrObject> elementsToMove) {
        List<KotlinMemberInfo> memberInfos = CollectionsKt.mapNotNull(
                originalClass.getDeclarations(),
                new Function1<KtDeclaration, KotlinMemberInfo>() {
                    @Override
                    public KotlinMemberInfo invoke(KtDeclaration declaration) {
                        if (!(declaration instanceof KtClassOrObject)) return null;

                        KotlinMemberInfo memberInfo = new KotlinMemberInfo((KtClassOrObject) declaration, false);
                        memberInfo.setChecked(elementsToMove.contains(declaration));
                        return memberInfo;
                    }
                }
        );
        KotlinMemberSelectionPanel selectionPanel = new KotlinMemberSelectionPanel(getTitle(), memberInfos, null);
        memberTable = selectionPanel.getTable();
        MemberInfoModelImpl memberInfoModel = new MemberInfoModelImpl();
        memberInfoModel.memberInfoChanged(new MemberInfoChange<KtNamedDeclaration, KotlinMemberInfo>(memberInfos));
        selectionPanel.getTable().setMemberInfoModel(memberInfoModel);
        selectionPanel.getTable().addMemberInfoChangeListener(memberInfoModel);
        membersInfoPanel.add(selectionPanel, BorderLayout.CENTER);
    }

    private List<KtClassOrObject> getSelectedElementsToMove() {
        return CollectionsKt.map(
                memberTable.getSelectedMemberInfos(),
                new Function1<KotlinMemberInfo, KtClassOrObject>() {
                    @Override
                    public KtClassOrObject invoke(KotlinMemberInfo info) {
                        return (KtClassOrObject) info.getMember();
                    }
                }
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

    @Override
    protected void canRun() throws ConfigurationException {
        if (targetClass == null) throw new ConfigurationException("No destination class specified");

        if (originalClass == targetClass) {
            throw new ConfigurationException(RefactoringBundle.message("source.and.destination.classes.should.be.different"));
        }

        for (KtClassOrObject classOrObject : getSelectedElementsToMove()) {
            if (PsiTreeUtil.isAncestor(classOrObject, targetClass, false)) {
                throw new ConfigurationException("Cannot move nested class " + classOrObject.getName() + " to itself");
            }
        }
    }

    @Override
    protected void doAction() {
        List<KtClassOrObject> elementsToMove = getSelectedElementsToMove();
        KotlinMoveTarget target = new KotlinMoveTargetForExistingElement(targetClass);
        MoveDeclarationsDelegate.NestedClass delegate = new MoveDeclarationsDelegate.NestedClass();
        MoveDeclarationsDescriptor descriptor = new MoveDeclarationsDescriptor(
                elementsToMove, target, delegate, false, false, true, false, moveCallback, openInEditorCheckBox.isSelected()
        );
        invokeRefactoring(new MoveKotlinDeclarationsProcessor(myProject, descriptor, Mover.Default.INSTANCE));
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return targetClassChooser.getChildComponent();
    }
}
