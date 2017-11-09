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

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceTypeAlias.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.ui.NameSuggestionsField;
import com.intellij.ui.TitledSeparator;
import com.intellij.util.containers.MultiMap;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinFileType;
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester;
import org.jetbrains.kotlin.idea.core.UtilsKt;
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringUtilKt;
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceTypeAlias.IntroduceTypeAliasDescriptor;
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceTypeAlias.IntroduceTypeAliasImplKt;
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceTypeAlias.KotlinIntroduceTypeAliasHandler;
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceTypeAlias.TypeParameter;
import org.jetbrains.kotlin.idea.refactoring.introduce.ui.KotlinSignatureComponent;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.lexer.KtTokens;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.*;
import java.util.List;

public class KotlinIntroduceTypeAliasDialog extends DialogWrapper {
    private JPanel contentPane;
    private TitledSeparator inputParametersPanel;
    private JComboBox visibilityBox;
    private KotlinSignatureComponent signaturePreviewField;
    private JPanel aliasNamePanel;
    private NameSuggestionsField aliasNameField;
    private JLabel aliasNameLabel;
    private IntroduceTypeAliasParameterTablePanel parameterTablePanel;

    private final Project project;

    private final IntroduceTypeAliasDescriptor originalDescriptor;
    private IntroduceTypeAliasDescriptor currentDescriptor;

    private final Function1<KotlinIntroduceTypeAliasDialog, Unit> onAccept;

    public KotlinIntroduceTypeAliasDialog(
            @NotNull Project project,
            @NotNull IntroduceTypeAliasDescriptor originalDescriptor,
            @NotNull Function1<KotlinIntroduceTypeAliasDialog, Unit> onAccept) {
        super(project, true);

        this.project = project;
        this.originalDescriptor = originalDescriptor;
        this.currentDescriptor = originalDescriptor;
        this.onAccept = onAccept;

        setModal(true);
        setTitle(KotlinIntroduceTypeAliasHandler.REFACTORING_NAME);
        init();
        update();
    }

    private void createUIComponents() {
        this.signaturePreviewField = new KotlinSignatureComponent("", project);
    }

    private boolean isVisibilitySectionAvailable() {
        return !getApplicableVisibilities().isEmpty();
    }

    @NotNull
    private List<KtModifierKeywordToken> getApplicableVisibilities() {
        return IntroduceTypeAliasImplKt.getApplicableVisibilities(originalDescriptor.getOriginalData());
    }

    private String getAliasName() {
        return UtilsKt.quoteIfNeeded(aliasNameField.getEnteredName());
    }

    @Nullable
    private KtModifierKeywordToken getVisibility() {
        if (!isVisibilitySectionAvailable()) return null;
        return (KtModifierKeywordToken) visibilityBox.getSelectedItem();
    }

    private boolean checkNames() {
        if (!KotlinNameSuggester.INSTANCE.isIdentifier(getAliasName())) return false;
        if (parameterTablePanel != null) {
            for (IntroduceTypeAliasParameterTablePanel.TypeParameterInfo parameterInfo : parameterTablePanel.getSelectedTypeParameterInfos()) {
                if (!KotlinNameSuggester.INSTANCE.isIdentifier(parameterInfo.getName())) return false;
            }
        }
        return true;
    }

    private void update() {
        this.currentDescriptor = createDescriptor();

        setOKActionEnabled(checkNames());
        signaturePreviewField.setText(IntroduceTypeAliasImplKt.generateTypeAlias(currentDescriptor, true).getText());
    }

    @Override
    protected void init() {
        super.init();

        //noinspection unchecked
        visibilityBox.setModel(new DefaultComboBoxModel(getApplicableVisibilities().toArray()));
        //noinspection unchecked
        visibilityBox.setRenderer(
                new DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(
                            JList list,
                            Object value,
                            int index,
                            boolean isSelected,
                            boolean cellHasFocus
                    ) {
                        String tokenValue = value != null ? ((KtModifierKeywordToken) value).getValue() : null;
                        return super.getListCellRendererComponent(list, tokenValue, index, isSelected, cellHasFocus);
                    }
                }
        );

        aliasNameField = new NameSuggestionsField(new String[]{originalDescriptor.getName()}, project, KotlinFileType.INSTANCE);
        aliasNameField.addDataChangedListener(
                new NameSuggestionsField.DataChanged() {
                    @Override
                    public void dataChanged() {
                        update();
                    }
                }
        );
        aliasNamePanel.add(aliasNameField, BorderLayout.CENTER);
        aliasNameLabel.setLabelFor(aliasNameField);

        boolean enableVisibility = isVisibilitySectionAvailable();
        visibilityBox.setEnabled(enableVisibility);
        if (enableVisibility) {
            KtModifierKeywordToken defaultVisibility = originalDescriptor.getVisibility();
            if (defaultVisibility == null) {
                defaultVisibility = KtTokens.PUBLIC_KEYWORD;
            }
            visibilityBox.setSelectedItem(defaultVisibility);
        }
        visibilityBox.addItemListener(
                new ItemListener() {
                    @Override
                    public void itemStateChanged(@NotNull ItemEvent e) {
                        update();
                    }
                }
        );

        if (!originalDescriptor.getTypeParameters().isEmpty()) {
            parameterTablePanel = new IntroduceTypeAliasParameterTablePanel() {
                @Override
                protected void updateSignature() {
                    KotlinIntroduceTypeAliasDialog.this.update();
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
            parameterTablePanel.init(originalDescriptor.getTypeParameters());

            inputParametersPanel.setText("Type &Parameters");
            inputParametersPanel.setLabelFor(parameterTablePanel.getTable());
            inputParametersPanel.add(parameterTablePanel);
        }
        else {
            inputParametersPanel.setVisible(false);
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    protected void doOKAction() {
        MultiMap<PsiElement, String> conflicts = IntroduceTypeAliasImplKt.validate(currentDescriptor).getConflicts();
        KotlinRefactoringUtilKt.checkConflictsInteractively(
                project,
                conflicts,
                new Function0<Unit>() {
                    @Override
                    public Unit invoke() {
                        close(OK_EXIT_CODE);
                        return Unit.INSTANCE;
                    }
                },
                new Function0<Unit>() {
                    @Override
                    public Unit invoke() {
                        KotlinIntroduceTypeAliasDialog.super.doOKAction();
                        return onAccept.invoke(KotlinIntroduceTypeAliasDialog.this);
                    }
                }
        );
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return aliasNameField;
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
    private IntroduceTypeAliasDescriptor createDescriptor() {
        return originalDescriptor.copy(
                originalDescriptor.getOriginalData(),
                getAliasName(),
                getVisibility(),
                parameterTablePanel != null ? parameterTablePanel.getSelectedTypeParameters() : Collections.<TypeParameter>emptyList()
        );
    }

    public IntroduceTypeAliasDescriptor getCurrentDescriptor() {
        return currentDescriptor;
    }
}
