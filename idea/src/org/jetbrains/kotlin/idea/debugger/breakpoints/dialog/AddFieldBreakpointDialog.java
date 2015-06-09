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

package org.jetbrains.kotlin.idea.debugger.breakpoints.dialog;

import com.intellij.codeInsight.generation.PsiFieldMember;
import com.intellij.debugger.DebuggerBundle;
import com.intellij.ide.util.MemberChooser;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public abstract class AddFieldBreakpointDialog extends DialogWrapper {
    private final Project myProject;
    private JPanel myPanel;
    private TextFieldWithBrowseButton myFieldChooser;
    private TextFieldWithBrowseButton myClassChooser;

    public AddFieldBreakpointDialog(Project project) {
        super(project, true);
        myProject = project;
        setTitle(DebuggerBundle.message("add.field.breakpoint.dialog.title"));
        init();
    }

    protected JComponent createCenterPanel() {
        myClassChooser.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            public void textChanged(DocumentEvent event) {
                updateUI();
            }
        });

        myClassChooser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PsiClass currentClass = getSelectedClass();
                TreeClassChooser chooser = TreeClassChooserFactory.getInstance(myProject).createAllProjectScopeChooser(
                        DebuggerBundle.message("add.field.breakpoint.dialog.classchooser.title"));
                if (currentClass != null) {
                    PsiFile containingFile = currentClass.getContainingFile();
                    if (containingFile != null) {
                        PsiDirectory containingDirectory = containingFile.getContainingDirectory();
                        if (containingDirectory != null) {
                            chooser.selectDirectory(containingDirectory);
                        }
                    }
                }
                chooser.showDialog();
                PsiClass selectedClass = chooser.getSelected();
                if (selectedClass != null) {
                    myClassChooser.setText(selectedClass.getQualifiedName());
                }
            }
        });

        myFieldChooser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PsiClass selectedClass = getSelectedClass();
                if (selectedClass != null) {
                    PsiField[] fields = selectedClass.getFields();
                    MemberChooser<PsiFieldMember> chooser = new MemberChooser<PsiFieldMember>(
                            ContainerUtil.map2Array(fields, PsiFieldMember.class, new Function<PsiField, PsiFieldMember>() {
                                public PsiFieldMember fun(final PsiField s) {
                                    return new PsiFieldMember(s);
                                }
                            }), false, false, myProject);
                    chooser.setTitle(DebuggerBundle.message("add.field.breakpoint.dialog.field.chooser.title", fields.length));
                    chooser.setCopyJavadocVisible(false);
                    chooser.show();
                    List<PsiFieldMember> selectedElements = chooser.getSelectedElements();
                    if (selectedElements != null && selectedElements.size() == 1) {
                        PsiField field = selectedElements.get(0).getElement();
                        myFieldChooser.setText(field.getName());
                    }
                }
            }
        });
        myFieldChooser.setEnabled(false);
        return myPanel;
    }

    private void updateUI() {
        PsiClass selectedClass = getSelectedClass();
        myFieldChooser.setEnabled(selectedClass != null);
    }

    private PsiClass getSelectedClass() {
        PsiManager psiManager = PsiManager.getInstance(myProject);
        String classQName = myClassChooser.getText();
        if (classQName == null || classQName.isEmpty()) {
            return null;
        }
        return JavaPsiFacade.getInstance(psiManager.getProject()).findClass(classQName, GlobalSearchScope.allScope(myProject));
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return myClassChooser.getTextField();
    }

    public String getClassName() {
        return myClassChooser.getText();
    }

    @Override
    protected String getDimensionServiceKey() {
        return "#com.intellij.debugger.ui.breakpoints.BreakpointsConfigurationDialogFactory.BreakpointsConfigurationDialog.AddFieldBreakpointDialog";
    }

    public String getFieldName() {
        return myFieldChooser.getText();
    }

    protected abstract boolean validateData();

    @Override
    protected void doOKAction() {
        if (validateData()) {
            super.doOKAction();
        }
    }
}
