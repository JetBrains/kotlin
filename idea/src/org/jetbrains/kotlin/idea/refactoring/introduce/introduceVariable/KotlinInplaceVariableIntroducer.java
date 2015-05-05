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

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable;

import com.intellij.codeInsight.template.TemplateBuilderImpl;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.util.*;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.search.SearchScope;
import com.intellij.refactoring.introduce.inplace.InplaceVariableIntroducer;
import com.intellij.ui.NonFocusableCheckBox;
import com.intellij.util.ui.PositionTracker;
import kotlin.Function0;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.intentions.SpecifyTypeExplicitlyIntention;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.types.JetType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public class KotlinInplaceVariableIntroducer<D extends JetCallableDeclaration> extends InplaceVariableIntroducer<JetExpression> {
    private static final Key<KotlinInplaceVariableIntroducer> ACTIVE_INTRODUCER = Key.create("ACTIVE_INTRODUCER");

    public static final String TYPE_REFERENCE_VARIABLE_NAME = "TypeReferenceVariable";
    public static final String PRIMARY_VARIABLE_NAME = "PrimaryVariable";
    
    private static final Function0<Boolean> TRUE = new Function0<Boolean>() {
        @Override
        public Boolean invoke() {
            return true;
        }
    };

    private static final Pass<JComponent> DO_NOTHING = new Pass<JComponent>() {
        @Override
        public void pass(JComponent component) {

        }
    };

    protected static final class ControlWrapper {
        @NotNull
        private final Function0<JComponent> factory;
        @NotNull
        private final Function0<Boolean> condition;
        @NotNull
        private final Pass<JComponent> initializer;
        private JComponent component;

        public ControlWrapper(
                @NotNull Function0<JComponent> factory,
                @NotNull Function0<Boolean> condition,
                @NotNull Pass<JComponent> initializer) {
            this.factory = factory;
            this.condition = condition;
            this.initializer = initializer;
        }

        public ControlWrapper(@NotNull Function0<JComponent> factory) {
            this(factory, TRUE, DO_NOTHING);
        }

        public boolean isAvailable() {
            return condition.invoke();
        }

        public void initialize() {
            initializer.pass(getComponent());
        }

        @NotNull
        public JComponent getComponent() {
            if (component == null) {
                component = factory.invoke();
            }
            return component;
        }
    }

    private final boolean myReplaceOccurrence;
    protected D myDeclaration;
    private final boolean isVar;
    private final boolean myDoNotChangeVar;
    @Nullable private final JetType myExprType;
    private final boolean noTypeInference;
    private final List<ControlWrapper> panelControls = new ArrayList<ControlWrapper>();
    private JPanel contentPanel;

    public KotlinInplaceVariableIntroducer(
            PsiNamedElement elementToRename, Editor editor, Project project,
            String title, JetExpression[] occurrences,
            @Nullable JetExpression expr, boolean replaceOccurrence,
            D declaration, boolean isVar, boolean doNotChangeVar,
            @Nullable JetType exprType, boolean noTypeInference
    ) {
        super(elementToRename, editor, project, title, occurrences, expr);
        this.myReplaceOccurrence = replaceOccurrence;
        myDeclaration = declaration;
        this.isVar = isVar;
        myDoNotChangeVar = doNotChangeVar;
        myExprType = exprType;
        this.noTypeInference = noTypeInference;

        String advertisementActionId = getAdvertisementActionId();
        if (advertisementActionId != null) {
            showDialogAdvertisement(advertisementActionId);
        }
    }

    @Nullable
    protected String getAdvertisementActionId() {
        return null;
    }

    @NotNull
    private JPanel getContentPanel() {
        if (contentPanel == null) {
            contentPanel = new JPanel(new GridBagLayout());
            contentPanel.setBorder(null);
        }

        return contentPanel;
    }

    protected final void addPanelControl(@NotNull ControlWrapper panelControl) {
        panelControls.add(panelControl);
    }

    protected final void addPanelControl(@Nullable Function0<JComponent> initializer) {
        if (initializer != null) {
            addPanelControl(new ControlWrapper(initializer));
        }
    }

    protected void initPanelControls() {
        addPanelControl(getCreateVarCheckBox());
        addPanelControl(getCreateExplicitTypeCheckBox());
    }

    protected final void updatePanelControls() {
        JPanel panel = getContentPanel();

        panel.removeAll();

        int count = 1;
        for (ControlWrapper panelControl : panelControls) {
            if (!panelControl.isAvailable()) continue;
            panelControl.initialize();
            panel.add(panelControl.getComponent(), new GridBagConstraints(0, count, 1, 1, 1, 0, GridBagConstraints.NORTHWEST,
                                                                          GridBagConstraints.HORIZONTAL,
                                                                          new Insets(0, 5, 0, 5), 0, 0));
            ++count;
        }
        panel.add(Box.createVerticalBox(), new GridBagConstraints(0, count, 1, 1, 1, 1, GridBagConstraints.NORTHWEST,
                                                                  GridBagConstraints.BOTH,
                                                                  new Insets(0, 0, 0, 0), 0, 0));
    }

    @Override
    @Nullable
    protected final JComponent getComponent() {
        panelControls.clear();
        initPanelControls();

        updatePanelControls();

        return getContentPanel();
    }

    @Nullable
    protected final Function0<JComponent> getCreateExplicitTypeCheckBox() {
        if (myExprType == null || noTypeInference) return null;

        return new Function0<JComponent>() {
            @Override
            public JComponent invoke() {
                final JCheckBox exprTypeCheckbox = new NonFocusableCheckBox("Specify type explicitly");
                exprTypeCheckbox.setSelected(false);
                exprTypeCheckbox.setMnemonic('t');
                exprTypeCheckbox.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(@NotNull ActionEvent e) {
                        runWriteActionAndRestartRefactoring(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        if (exprTypeCheckbox.isSelected()) {
                                            String renderedType =
                                                    IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(myExprType);
                                            myDeclaration.setTypeReference(new JetPsiFactory(myProject).createType(renderedType));
                                        }
                                        else {
                                            myDeclaration.setTypeReference(null);
                                        }
                                    }
                                }
                        );
                    }
                });

                return exprTypeCheckbox;
            }
        };
    }

    @Nullable
    protected final Function0<JComponent> getCreateVarCheckBox() {
        if (myDoNotChangeVar) return null;

        return new Function0<JComponent>() {
            @Override
            public JComponent invoke() {
                final JCheckBox varCheckbox = new NonFocusableCheckBox("Declare with var");
                varCheckbox.setSelected(isVar);
                varCheckbox.setMnemonic('v');
                varCheckbox.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(@NotNull ActionEvent e) {
                        new WriteCommandAction(myProject, getCommandName(), getCommandName()) {
                            @Override
                            protected void run(@NotNull Result result) throws Throwable {
                                PsiDocumentManager.getInstance(myProject).commitDocument(myEditor.getDocument());

                                JetPsiFactory psiFactory = new JetPsiFactory(myProject);
                                ASTNode node = varCheckbox.isSelected() ? psiFactory.createVarNode() : psiFactory.createValNode();

                                ASTNode valOrVarNode = myDeclaration instanceof JetProperty
                                                       ? ((JetProperty) myDeclaration).getValOrVarNode()
                                                       : ((JetParameter) myDeclaration).getValOrVarNode();
                                valOrVarNode.getPsi().replace(node.getPsi());
                            }
                        }.execute();
                    }
                });

                return varCheckbox;
            }
        };
    }

    protected final void runWriteActionAndRestartRefactoring(final Runnable runnable) {
        final Ref<Boolean> greedyToRight = new Ref<Boolean>();
        new WriteCommandAction(myProject, getCommandName(), getCommandName()) {
            @Override
            protected void run(@NotNull Result result) throws Throwable {
                PsiDocumentManager.getInstance(myProject).commitDocument(myEditor.getDocument());

                ASTNode identifier = myDeclaration.getNode().findChildByType(JetTokens.IDENTIFIER);
                if (identifier != null) {
                    TextRange range = identifier.getTextRange();
                    RangeHighlighter[] highlighters = myEditor.getMarkupModel().getAllHighlighters();
                    for (RangeHighlighter highlighter : highlighters) {
                        if (highlighter.getStartOffset() == range.getStartOffset()) {
                            if (highlighter.getEndOffset() == range.getEndOffset()) {
                                greedyToRight.set(highlighter.isGreedyToRight());
                                highlighter.setGreedyToRight(false);
                            }
                        }
                    }
                }

                runnable.run();

                TemplateState templateState =
                        TemplateManagerImpl.getTemplateState(InjectedLanguageUtil.getTopLevelEditor(myEditor));
                if (templateState != null) {
                    myEditor.putUserData(INTRODUCE_RESTART, true);
                    templateState.cancelTemplate();
                }
            }
        }.execute();
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                ASTNode identifier = myDeclaration.getNode().findChildByType(JetTokens.IDENTIFIER);
                if (identifier != null) {
                    TextRange range = identifier.getTextRange();
                    RangeHighlighter[] highlighters = myEditor.getMarkupModel().getAllHighlighters();
                    for (RangeHighlighter highlighter : highlighters) {
                        if (highlighter.getStartOffset() == range.getStartOffset()) {
                            if (highlighter.getEndOffset() == range.getEndOffset()) {
                                highlighter.setGreedyToRight(greedyToRight.get());
                            }
                        }
                    }
                }
            }
        });

        if (myEditor.getUserData(INTRODUCE_RESTART) == Boolean.TRUE) {
            myInitialName = myDeclaration.getName();
            performInplaceRefactoring(getSuggestionsForNextRun());
        }
    }

    private LinkedHashSet<String> getSuggestionsForNextRun() {
        LinkedHashSet<String> nameSuggestions;
        String currentName = myDeclaration.getName();
        if (myNameSuggestions.contains(currentName)) {
            nameSuggestions = myNameSuggestions;
        }
        else {
            nameSuggestions = new LinkedHashSet<String>();
            nameSuggestions.add(currentName);
            nameSuggestions.addAll(myNameSuggestions);
        }
        return nameSuggestions;
    }

    protected void revalidate() {
        getContentPanel().revalidate();
        if (myTarget != null) {
            myBalloon.revalidate(new PositionTracker.Static<Balloon>(myTarget));
        }
    }

    protected void addTypeReferenceVariable(TemplateBuilderImpl builder) {
        JetTypeReference typeReference = myDeclaration.getTypeReference();
        if (typeReference != null) {
            builder.replaceElement(typeReference,
                                   TYPE_REFERENCE_VARIABLE_NAME,
                                   SpecifyTypeExplicitlyIntention.Companion.createTypeExpressionForTemplate(myExprType),
                                   false);
        }
    }

    @Override
    protected void addAdditionalVariables(TemplateBuilderImpl builder) {
        addTypeReferenceVariable(builder);
    }

    @Override
    protected boolean buildTemplateAndStart(
            Collection<PsiReference> refs,
            Collection<Pair<PsiElement, TextRange>> stringUsages,
            PsiElement scope,
            PsiFile containingFile
    ) {
        myEditor.putUserData(INTRODUCE_RESTART, false);
        //noinspection ConstantConditions
        myEditor.getCaretModel().moveToOffset(getNameIdentifier().getTextOffset());
        boolean result = super.buildTemplateAndStart(refs, stringUsages, scope, containingFile);

        TemplateState templateState =
                TemplateManagerImpl.getTemplateState(InjectedLanguageUtil.getTopLevelEditor(myEditor));
        if (templateState != null && myDeclaration.getTypeReference() != null) {
            templateState.addTemplateStateListener(SpecifyTypeExplicitlyIntention.Companion.createTypeReferencePostprocessor(myDeclaration));
        }

        return result;
    }

    @Override
    protected Collection<PsiReference> collectRefs(SearchScope referencesSearchScope) {
        return KotlinPackage.map(
                KotlinPackage.filterIsInstance(getOccurrences(), JetSimpleNameExpression.class),
                new Function1<JetSimpleNameExpression, PsiReference>() {
                    @Override
                    public PsiReference invoke(JetSimpleNameExpression expression) {
                        return expression.getReference();
                    }
                }
        );
    }

    @Override
    public boolean performInplaceRefactoring(LinkedHashSet<String> nameSuggestions) {
        if (super.performInplaceRefactoring(nameSuggestions)) {
            myEditor.putUserData(ACTIVE_INTRODUCER, this);
            return true;
        }
        return false;
    }

    @Override
    public void finish(boolean success) {
        super.finish(success);
        myEditor.putUserData(ACTIVE_INTRODUCER, null);
    }

    @Override
    protected void moveOffsetAfter(boolean success) {
        if (!myReplaceOccurrence || myExprMarker == null) {
            myEditor.getCaretModel().moveToOffset(myDeclaration.getTextRange().getEndOffset());
        }
        else {
            int startOffset = myExprMarker.getStartOffset();
            PsiFile file = myDeclaration.getContainingFile();
            PsiElement elementAt = file.findElementAt(startOffset);
            if (elementAt != null) {
                myEditor.getCaretModel().moveToOffset(elementAt.getTextRange().getEndOffset());
            }
            else {
                myEditor.getCaretModel().moveToOffset(myExprMarker.getEndOffset());
            }
        }
    }

    public void stopIntroduce() {
        final TemplateState templateState = TemplateManagerImpl.getTemplateState(myEditor);
        if (templateState != null) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    templateState.gotoEnd(true);
                }
            };
            CommandProcessor.getInstance().executeCommand(myProject, runnable, getCommandName(), getCommandName());
        }
    }

    @Nullable
    public static KotlinInplaceVariableIntroducer getActiveInstance(@NotNull Editor editor) {
        return editor.getUserData(ACTIVE_INTRODUCER);
    }
}
