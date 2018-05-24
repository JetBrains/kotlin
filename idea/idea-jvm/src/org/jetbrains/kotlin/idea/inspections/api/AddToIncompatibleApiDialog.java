/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.api;

import com.intellij.codeInspection.ex.InspectionProfileModifiableModelKt;
import com.intellij.codeInspection.ex.ToolsImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class AddToIncompatibleApiDialog extends DialogWrapper {
    private JPanel contentPane;
    private JTextField referenceTextField;
    private JTextField reasonTextField;

    private final Project project;

    public AddToIncompatibleApiDialog(@NotNull Project project, @NotNull String qualifiedReference) {
        super(project, true);
        this.project = project;

        setModal(true);
        setTitle("Report as Incompatible API");
        referenceTextField.setText(qualifiedReference);

        init();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return reasonTextField;
    }

    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    public Container getContentPane() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        String reference = referenceTextField.getText();
        String reason = reasonTextField.getText();

        reference = reference != null ? reference.trim() : null;
        reason = reason != null ? reason.trim() : null;

        if (reference != null && !reference.isEmpty()) {
            String finalReference = reference;
            String finalReason = reason == null || reason.isEmpty() ? null : reason;

            InspectionProfileModifiableModelKt.modifyAndCommitProjectProfile(project, inspectionProfileModifiableModel -> {
                ToolsImpl toolImpl = inspectionProfileModifiableModel.getToolsOrNull(IncompatibleAPIInspection.SHORT_NAME, project);
                if (toolImpl != null) {
                    IncompatibleAPIInspection incompatibleAPIInspection =
                            (IncompatibleAPIInspection) toolImpl.getDefaultState().getTool().getTool();
                    incompatibleAPIInspection.addProblem(finalReference, finalReason);
                }
            });
        }

        super.doOKAction();
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
