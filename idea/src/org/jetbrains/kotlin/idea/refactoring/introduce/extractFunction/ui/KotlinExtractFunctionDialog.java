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

package org.jetbrains.kotlin.idea.refactoring.introduce.extractFunction.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.ui.NameSuggestionsField;
import com.intellij.ui.TitledSeparator;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import kotlin.Function0;
import kotlin.Function1;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.JetFileType;
import org.jetbrains.kotlin.idea.refactoring.JetNameSuggester;
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringBundle;
import org.jetbrains.kotlin.idea.refactoring.RefactoringPackage;
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.*;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class KotlinExtractFunctionDialog extends DialogWrapper {
    private JPanel contentPane;
    private TitledSeparator inputParametersPanel;
    private JComboBox visibilityBox;
    private KotlinFunctionSignatureComponent signaturePreviewField;
    private JPanel functionNamePanel;
    private NameSuggestionsField functionNameField;
    private JLabel functionNameLabel;
    private KotlinParameterTablePanel parameterTablePanel;

    private final Project project;

    private final ExtractableCodeDescriptorWithConflicts originalDescriptor;
    private ExtractableCodeDescriptor currentDescriptor;

    private final Function1<KotlinExtractFunctionDialog, Unit> onAccept;

    public KotlinExtractFunctionDialog(
            @NotNull Project project,
            @NotNull ExtractableCodeDescriptorWithConflicts originalDescriptor,
            @NotNull Function1<KotlinExtractFunctionDialog, Unit> onAccept) {
        super(project, true);

        this.project = project;
        this.originalDescriptor = originalDescriptor;
        this.currentDescriptor = originalDescriptor.getDescriptor();
        this.onAccept = onAccept;

        setModal(true);
        setTitle(JetRefactoringBundle.message("extract.function"));
        init();
        update();
    }

    private void createUIComponents() {
        this.signaturePreviewField = new KotlinFunctionSignatureComponent("", project);
    }

    private boolean isVisibilitySectionAvailable() {
        return ExtractionEnginePackage.isVisibilityApplicable(originalDescriptor.getDescriptor().getExtractionData());
    }

    private String getFunctionName() {
        return functionNameField.getEnteredName();
    }

    private String getVisibility() {
        if (!isVisibilitySectionAvailable()) return "";

        String value = (String) visibilityBox.getSelectedItem();
        return "internal".equals(value) ? "" : value;
    }

    private boolean checkNames() {
        if (!JetNameSuggester.isIdentifier(getFunctionName())) return false;
        for (KotlinParameterTablePanel.ParameterInfo parameterInfo : parameterTablePanel.getParameterInfos()) {
            if (!JetNameSuggester.isIdentifier(parameterInfo.getName())) return false;
        }
        return true;
    }

    private void update() {
        this.currentDescriptor = createDescriptor();

        setOKActionEnabled(checkNames());
        signaturePreviewField.setText(
                ExtractionEnginePackage.getDeclarationText(getCurrentConfiguration(),
                                                           false,
                                                           IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES)
        );
    }

    @Override
    protected void init() {
        super.init();

        functionNameField = new NameSuggestionsField(
                ArrayUtil.toStringArray(originalDescriptor.getDescriptor().getSuggestedNames()),
                project,
                JetFileType.INSTANCE
        );
        functionNameField.addDataChangedListener(
                new NameSuggestionsField.DataChanged() {
                    @Override
                    public void dataChanged() {
                        update();
                    }
                }
        );
        functionNamePanel.add(functionNameField, BorderLayout.CENTER);
        functionNameLabel.setLabelFor(functionNameField);

        boolean enableVisibility = isVisibilitySectionAvailable();
        visibilityBox.setEnabled(enableVisibility);
        if (enableVisibility) {
            visibilityBox.setSelectedItem(originalDescriptor.getDescriptor().getVisibility());
        }
        visibilityBox.addItemListener(
                new ItemListener() {
                    @Override
                    public void itemStateChanged(@NotNull ItemEvent e) {
                        update();
                    }
                }
        );

        parameterTablePanel = new KotlinParameterTablePanel() {
            @Override
            protected void updateSignature() {
                KotlinExtractFunctionDialog.this.update();
            }

            @Override
            protected void onEnterAction() {
                doOKAction();
            }

            @Override
            protected void onCancelAction() {
                doCancelAction();
            }
        };
        parameterTablePanel.init(originalDescriptor.getDescriptor().getParameters());

        inputParametersPanel.setText("&Parameters");
        inputParametersPanel.setLabelFor(parameterTablePanel.getTable());
        inputParametersPanel.add(parameterTablePanel);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    protected void doOKAction() {
        MultiMap<PsiElement, String> conflicts = ExtractionEnginePackage.validate(currentDescriptor).getConflicts();
        conflicts.values().removeAll(originalDescriptor.getConflicts().values());

        RefactoringPackage.checkConflictsInteractively(
                project,
                conflicts,
                new Function0<Unit>() {
                    @Override
                    public Unit invoke() {
                        KotlinExtractFunctionDialog.super.doOKAction();
                        return onAccept.invoke(KotlinExtractFunctionDialog.this);
                    }
                }
        );
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return functionNameField;
    }

    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @NotNull
    @Override
    protected JComponent createContentPane() {
        return contentPane;
    }

    @NotNull
    private ExtractableCodeDescriptor createDescriptor() {
        ExtractableCodeDescriptor descriptor = originalDescriptor.getDescriptor();

        List<KotlinParameterTablePanel.ParameterInfo> parameterInfos = parameterTablePanel.getParameterInfos();

        Map<Parameter, Parameter> oldToNewParameters = ContainerUtil.newLinkedHashMap();
        for (KotlinParameterTablePanel.ParameterInfo parameterInfo : parameterInfos) {
            oldToNewParameters.put(parameterInfo.getOriginalParameter(), parameterInfo.toParameter());
        }

        ControlFlow controlFlow = descriptor.getControlFlow();
        List<OutputValue> outputValues = new ArrayList<OutputValue>(controlFlow.getOutputValues());
        for (int i = 0; i < outputValues.size(); i++) {
            OutputValue outputValue = outputValues.get(i);
            if (outputValue instanceof OutputValue.ParameterUpdate) {
                OutputValue.ParameterUpdate parameterUpdate = (OutputValue.ParameterUpdate) outputValue;
                outputValues.set(i, new OutputValue.ParameterUpdate(oldToNewParameters.get(parameterUpdate.getParameter()), parameterUpdate.getOriginalExpressions()));
            }
        }
        controlFlow = new ControlFlow(outputValues, controlFlow.getBoxerFactory(), controlFlow.getDeclarationsToCopy());

        Map<Integer, Replacement> replacementMap = ContainerUtil.newHashMap();
        for (Map.Entry<Integer, Replacement> e : descriptor.getReplacementMap().entrySet()) {
            Integer offset = e.getKey();
            Replacement replacement = e.getValue();

            if (replacement instanceof ParameterReplacement) {
                ParameterReplacement parameterReplacement = (ParameterReplacement) replacement;
                Parameter parameter = parameterReplacement.getParameter();

                Parameter newParameter = oldToNewParameters.get(parameter);
                if (newParameter != null) {
                    replacementMap.put(offset, parameterReplacement.copy(newParameter));
                }
            }
            else {
                replacementMap.put(offset, replacement);
            }
        }

        return new ExtractableCodeDescriptor(
                descriptor.getExtractionData(),
                descriptor.getOriginalContext(),
                Collections.singletonList(getFunctionName()),
                getVisibility(),
                ContainerUtil.newArrayList(oldToNewParameters.values()),
                descriptor.getReceiverParameter(),
                descriptor.getTypeParameters(),
                replacementMap,
                controlFlow
        );
    }

    @NotNull
    public ExtractionGeneratorConfiguration getCurrentConfiguration() {
        return new ExtractionGeneratorConfiguration(currentDescriptor, ExtractionGeneratorOptions.DEFAULT);
    }
}
