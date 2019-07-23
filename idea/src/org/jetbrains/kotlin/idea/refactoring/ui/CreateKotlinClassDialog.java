/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.ui;

import com.intellij.CommonBundle;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.daemon.impl.quickfix.ClassKind;
import com.intellij.ide.util.PackageUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Pass;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiNameHelper;
import com.intellij.refactoring.MoveDestination;
import com.intellij.refactoring.PackageWrapper;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.move.moveClassesOrPackages.MultipleRootsMoveDestination;
import com.intellij.refactoring.ui.PackageNameReferenceEditorCombo;
import com.intellij.refactoring.util.RefactoringMessageUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.RecentsManager;
import com.intellij.ui.ReferenceEditorComboWithBrowseButton;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static org.jetbrains.kotlin.idea.roots.ProjectRootUtilsKt.getSuitableDestinationSourceRoots;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

// Based on com.intellij.codeInsight.intention.impl.CreateClassDialog
public class CreateKotlinClassDialog extends DialogWrapper {
  private final JLabel myInformationLabel = new JLabel("#");
  private final JLabel myPackageLabel = new JLabel(CodeInsightBundle.message("dialog.create.class.destination.package.label"));
  private final ReferenceEditorComboWithBrowseButton myPackageComponent;
  private final JTextField myTfClassName = new MyTextField();
  private final Project myProject;
  private PsiDirectory myTargetDirectory;
  private final String myClassName;
  private final boolean myClassNameEditable;
  private final Module myModule;
  private final KotlinDestinationFolderComboBox myDestinationCB = new KotlinDestinationFolderComboBox() {
    @Override
    public String getTargetPackage() {
      return myPackageComponent.getText().trim();
    }

    @Override
    protected boolean reportBaseInTestSelectionInSource() {
      return CreateKotlinClassDialog.this.reportBaseInTestSelectionInSource();
    }

    @Override
    protected boolean reportBaseInSourceSelectionInTest() {
      return CreateKotlinClassDialog.this.reportBaseInSourceSelectionInTest();
    }
  };
  @NonNls private static final String RECENTS_KEY = "CreateKotlinClassDialog.RecentsKey";

  public CreateKotlinClassDialog(@NotNull Project project,
                           @NotNull String title,
                           @NotNull String targetClassName,
                           @NotNull String targetPackageName,
                           @NotNull ClassKind kind,
                           boolean classNameEditable,
                           @Nullable Module defaultModule) {
    super(project, true);
    myClassNameEditable = classNameEditable;
    myModule = defaultModule;
    myClassName = targetClassName;
    myProject = project;
    myPackageComponent = new PackageNameReferenceEditorCombo(targetPackageName, myProject, RECENTS_KEY, CodeInsightBundle.message("dialog.create.class.package.chooser.title"));
    myPackageComponent.setTextFieldPreferredWidth(40);

    init();

    if (!myClassNameEditable) {
      setTitle(CodeInsightBundle.message("dialog.create.class.name", StringUtil.capitalize(kind.getDescription()), targetClassName));
    }
    else {
      myInformationLabel.setText(CodeInsightBundle.message("dialog.create.class.label", kind.getDescription()));
      setTitle(title);
    }

    myTfClassName.setText(myClassName);
    myDestinationCB.setData(myProject, getBaseDir(targetPackageName), new Pass<String>() {
      @Override
      public void pass(String s) {
        setErrorText(s, myDestinationCB);
      }
    }, myPackageComponent.getChildComponent());
  }

  protected boolean reportBaseInTestSelectionInSource() {
    return false;
  }

  protected boolean reportBaseInSourceSelectionInTest() {
    return false;
  }

  @NotNull
  @Override
  protected Action[] createActions() {
    return new Action[]{getOKAction(), getCancelAction()};
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myClassNameEditable ? myTfClassName : myPackageComponent.getChildComponent();
  }

  @Override
  protected JComponent createCenterPanel() {
    return new JPanel(new BorderLayout());
  }

  @Override
  protected JComponent createNorthPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();

    gbConstraints.insets = JBUI.insets(4, 8);
    gbConstraints.fill = GridBagConstraints.HORIZONTAL;
    gbConstraints.anchor = GridBagConstraints.WEST;

    if (myClassNameEditable) {
      gbConstraints.weightx = 0;
      gbConstraints.gridwidth = 1;
      panel.add(myInformationLabel, gbConstraints);
      gbConstraints.insets = JBUI.insets(4, 8);
      gbConstraints.gridx = 1;
      gbConstraints.weightx = 1;
      gbConstraints.gridwidth = 1;
      gbConstraints.fill = GridBagConstraints.HORIZONTAL;
      gbConstraints.anchor = GridBagConstraints.WEST;
      panel.add(myTfClassName, gbConstraints);

      myTfClassName.getDocument().addDocumentListener(new DocumentAdapter() {
        @Override
        protected void textChanged(DocumentEvent e) {
          getOKAction().setEnabled(PsiNameHelper.getInstance(myProject).isIdentifier(myTfClassName.getText()));
        }
      });
      getOKAction().setEnabled(StringUtil.isNotEmpty(myClassName));
    }

    gbConstraints.gridx = 0;
    gbConstraints.gridy = 2;
    gbConstraints.weightx = 0;
    gbConstraints.gridwidth = 1;
    panel.add(myPackageLabel, gbConstraints);

    gbConstraints.gridx = 1;
    gbConstraints.weightx = 1;

    new AnAction() {
      @Override
      public void actionPerformed(AnActionEvent e) {
        myPackageComponent.getButton().doClick();
      }
    }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK)), myPackageComponent.getChildComponent());

    JPanel _panel = new JPanel(new BorderLayout());
    _panel.add(myPackageComponent, BorderLayout.CENTER);
    panel.add(_panel, gbConstraints);

    gbConstraints.gridy = 3;
    gbConstraints.gridx = 0;
    gbConstraints.gridwidth = 2;
    gbConstraints.insets.top = 12;
    gbConstraints.anchor = GridBagConstraints.WEST;
    gbConstraints.fill = GridBagConstraints.NONE;
    JBLabel label = new JBLabel(RefactoringBundle.message("target.destination.folder"));
    panel.add(label, gbConstraints);

    gbConstraints.gridy = 4;
    gbConstraints.gridx = 0;
    gbConstraints.fill = GridBagConstraints.HORIZONTAL;
    gbConstraints.insets.top = 4;
    panel.add(myDestinationCB, gbConstraints);

    boolean isMultipleSourceRoots = getSuitableDestinationSourceRoots(myProject).size() > 1;
    myDestinationCB.setVisible(isMultipleSourceRoots);
    label.setVisible(isMultipleSourceRoots);
    label.setLabelFor(myDestinationCB);
    return panel;
  }

  public PsiDirectory getTargetDirectory() {
    return myTargetDirectory;
  }

  private String getPackageName() {
    String name = myPackageComponent.getText();
    return name != null ? name.trim() : "";
  }

  private static class MyTextField extends JTextField {
    @Override
    public Dimension getPreferredSize() {
      Dimension size = super.getPreferredSize();
      FontMetrics fontMetrics = getFontMetrics(getFont());
      size.width = fontMetrics.charWidth('a') * 40;
      return size;
    }
  }

  @Override
  protected void doOKAction() {
    RecentsManager.getInstance(myProject).registerRecentEntry(RECENTS_KEY, myPackageComponent.getText());
    String packageName = getPackageName();

    String[] errorString = new String[1];
    CommandProcessor.getInstance().executeCommand(myProject, () -> {
      try {
        PackageWrapper targetPackage = new PackageWrapper(PsiManager.getInstance(myProject), packageName);
        MoveDestination destination = myDestinationCB.selectDirectory(targetPackage, false);
        if (destination == null) return;
        myTargetDirectory = WriteAction.compute(() -> {
          PsiDirectory baseDir = getBaseDir(packageName);
          if (baseDir == null && destination instanceof MultipleRootsMoveDestination) {
            errorString[0] = "Destination not found for package '" + packageName + "'";
            return null;
          }
          return destination.getTargetDirectory(baseDir);
        });
        if (myTargetDirectory == null) {
          return;
        }
        errorString[0] = RefactoringMessageUtil.checkCanCreateClass(myTargetDirectory, getClassName());
      }
      catch (IncorrectOperationException e) {
        errorString[0] = e.getMessage();
      }
    }, CodeInsightBundle.message("create.directory.command"), null);

    if (errorString[0] != null) {
      if (errorString[0].length() > 0) {
        Messages.showMessageDialog(myProject, errorString[0], CommonBundle.getErrorTitle(), Messages.getErrorIcon());
      }
      return;
    }
    super.doOKAction();
  }

  @Nullable
  protected PsiDirectory getBaseDir(String packageName) {
    return myModule == null? null : PackageUtil.findPossiblePackageDirectoryInModule(myModule, packageName);
  }

  @NotNull
  public String getClassName() {
    if (myClassNameEditable) {
      return myTfClassName.getText();
    }
    else {
      return myClassName;
    }
  }
}
