/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.quickfix;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopupStep;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetCallElement;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetValueArgument;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.JetIcons;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public class AddNameToArgumentFix extends JetIntentionAction<JetValueArgument> {

    @NotNull
    private final List<String> possibleNames;

    public AddNameToArgumentFix(@NotNull JetValueArgument argument, @NotNull List<String> possibleNames) {
        super(argument);
        this.possibleNames = possibleNames;
    }

    @NotNull
    private static List<String> generatePossibleNames(@NotNull JetValueArgument argument) {
        JetCallElement callElement = PsiTreeUtil.getParentOfType(argument, JetCallElement.class);
        assert callElement != null : "The argument has to be inside a function or constructor call";

        BindingContext context = ResolvePackage.getBindingContext(argument.getContainingJetFile());
        ResolvedCall<?> resolvedCall = CallUtilPackage.getResolvedCall(callElement, context);
        if (resolvedCall == null) return Collections.emptyList();

        CallableDescriptor callableDescriptor = resolvedCall.getResultingDescriptor();
        JetType type = context.get(BindingContext.EXPRESSION_TYPE, argument.getArgumentExpression());
        Set<String> usedParameters = QuickFixUtil.getUsedParameters(callElement, null, callableDescriptor);
        List<String> names = Lists.newArrayList();
        for (ValueParameterDescriptor parameter: callableDescriptor.getValueParameters()) {
            String name = parameter.getName().asString();
            if (usedParameters.contains(name)) continue;
            if (type == null || JetTypeChecker.DEFAULT.isSubtypeOf(type, parameter.getType())) {
                names.add(name);
            }
        }
        return names;
    }

    @Override
    protected void invoke(@NotNull Project project, Editor editor, JetFile file) {
        if (possibleNames.size() == 1 || editor == null || !editor.getComponent().isShowing()) {
            addName(project, element, possibleNames.get(0));
        }
        else {
            chooseNameAndAdd(project, editor);
        }
    }

    private void chooseNameAndAdd(@NotNull Project project, Editor editor) {
        JBPopupFactory.getInstance().createListPopup(getNamePopup(project)).showInBestPositionFor(editor);
    }

    private ListPopupStep getNamePopup(final @NotNull Project project) {
        return new BaseListPopupStep<String>(
                JetBundle.message("add.name.to.parameter.name.chooser.title"), possibleNames) {
            @Override
            public PopupStep onChosen(String selectedName, boolean finalChoice) {
                if (finalChoice) {
                    addName(project, element, selectedName);
                }
                return FINAL_CHOICE;
            }

            @Override
            public Icon getIconFor(String name) {
                return JetIcons.PARAMETER;
            }

            @NotNull
            @Override
            public String getTextFor(String name) {
                return getParsedArgumentWithName(name, element).getText();
            }
        };
    }

    private static void addName(@NotNull Project project, final @NotNull JetValueArgument argument, final @NotNull String name) {
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        JetValueArgument newArgument = getParsedArgumentWithName(name, argument);
                        argument.replace(newArgument);
                    }
                });
            }
        }, JetBundle.message("add.name.to.argument.action"), null);
    }

    @NotNull
    private static JetValueArgument getParsedArgumentWithName(@NotNull String name, @NotNull JetValueArgument argument) {
        JetExpression argumentExpression = argument.getArgumentExpression();
        assert argumentExpression != null : "Argument should be already parsed.";
        return JetPsiFactory(argument).createArgumentWithName(name, argumentExpression);
    }

    @NotNull
    @Override
    public String getText() {
        if (possibleNames.size() == 1) {
            return JetBundle.message("add.name.to.argument.single", getParsedArgumentWithName(possibleNames.get(0), element).getText());
        } else {
            return JetBundle.message("add.name.to.argument.multiple");
        }
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("add.name.to.argument.family");
    }
    @NotNull
    public static JetIntentionActionsFactory createFactory() {
        return new JetSingleIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetValueArgument argument = QuickFixUtil.getParentElementOfType(diagnostic, JetValueArgument.class);
                if (argument == null) return null;
                List<String> possibleNames = generatePossibleNames(argument);
                return possibleNames.isEmpty() ? null : new AddNameToArgumentFix(argument, possibleNames);
            }
        };
    }
}
