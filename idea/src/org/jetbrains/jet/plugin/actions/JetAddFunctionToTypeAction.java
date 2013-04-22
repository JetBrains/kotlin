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

package org.jetbrains.jet.plugin.actions;

import com.intellij.codeInsight.hint.QuestionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopupStep;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetClassBody;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.codeInsight.CodeInsightUtils;
import org.jetbrains.jet.plugin.codeInsight.DescriptorToDeclarationUtil;
import org.jetbrains.jet.plugin.codeInsight.ReferenceToClassesShortening;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Changes method signature to one of provided signatures.
 * Based on {@link JetAddImportAction}
 */
public class JetAddFunctionToTypeAction implements QuestionAction {
    private final List<FunctionToAdd> functionsToAdd;
    private final Project project;
    private final Editor editor;
    private final BindingContext bindingContext;

    public static class FunctionToAdd {
        private final ClassDescriptor typeDescriptor;
        private final FunctionDescriptor functionDescriptor;

        public ClassDescriptor getTypeDescriptor() {
            return typeDescriptor;
        }

        public FunctionDescriptor getFunctionDescriptor() {
            return functionDescriptor;
        }

        public FunctionToAdd(FunctionDescriptor functionDescriptor, ClassDescriptor typeDescriptor) {
            this.functionDescriptor = functionDescriptor;
            this.typeDescriptor = typeDescriptor;
        }
    }

    /**
     * @param project Project where action takes place.
     * @param editor Editor where modification should be done.
     * @param bindingContext BindingContext to be used for finding type declarations.
     * @param functionsToAdd List of possible functions to add.
     */
    public JetAddFunctionToTypeAction(
            @NotNull Project project,
            @NotNull Editor editor,
            @NotNull BindingContext bindingContext,
            @NotNull List<FunctionToAdd> functionsToAdd
    ) {
        this.project = project;
        this.editor = editor;
        this.bindingContext = bindingContext;
        this.functionsToAdd = new ArrayList<FunctionToAdd>(functionsToAdd);
    }

    @Override
    public boolean execute() {
        if (functionsToAdd.isEmpty()) {
            return false;
        }

        if (functionsToAdd.size() == 1 || !editor.getComponent().isShowing()) {
            addFunction(functionsToAdd.get(0));
        }
        else {
            chooseFunctionAndAdd();
        }

        return true;
    }

    private void chooseFunctionAndAdd() {
        JBPopupFactory.getInstance().createListPopup(getFunctionPopup()).showInBestPositionFor(editor);
    }

    private ListPopupStep getFunctionPopup() {
        return new BaseListPopupStep<FunctionToAdd>(
                JetBundle.message("add.function.to.supertype.action.multiple"), functionsToAdd) {
            @Override
            public boolean isAutoSelectionEnabled() {
                return false;
            }

            @Override
            public PopupStep onChosen(FunctionToAdd selectedValue, boolean finalChoice) {
                if (finalChoice) {
                    addFunction(selectedValue);
                }
                return FINAL_CHOICE;
            }

            @Override
            public Icon getIconFor(FunctionToAdd aValue) {
                return PlatformIcons.FUNCTION_ICON;
            }

            @NotNull
            @Override
            public String getTextFor(FunctionToAdd aValue) {
                FunctionDescriptor newFunction = aValue.getFunctionDescriptor();
                ClassDescriptor type = aValue.getTypeDescriptor();
                return JetBundle.message("add.function.to.type.action.single",
                                         CodeInsightUtils.createFunctionSignatureStringFromDescriptor(
                                                 newFunction,
                                                 /* shortTypeNames = */ true),
                                         type.getName().toString());
            }
        };
    }

    private void addFunction(FunctionToAdd functionToAdd) {
        addFunction(project, functionToAdd.getTypeDescriptor(), functionToAdd.getFunctionDescriptor(), bindingContext);
    }

    private static void addFunction(
            final Project project,
            final ClassDescriptor typeDescriptor,
            @NotNull final FunctionDescriptor functionDescriptor,
            BindingContext bindingContext
    ) {
        final String signatureString = CodeInsightUtils.createFunctionSignatureStringFromDescriptor(
                functionDescriptor,
                /* shortTypeNames = */ false);

        PsiDocumentManager.getInstance(project).commitAllDocuments();

        final JetClass typeDeclaration = (JetClass) DescriptorToDeclarationUtil.getDeclaration(project, typeDescriptor, bindingContext);
        CommandProcessor.getInstance().executeCommand(project, new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        JetClassBody body = typeDeclaration.getBody();
                        if (body == null) {
                            PsiElement whitespaceBefore = typeDeclaration.add(JetPsiFactory.createWhiteSpace(project));
                            body = (JetClassBody) typeDeclaration.addAfter(JetPsiFactory.createEmptyClassBody(project), whitespaceBefore);
                            typeDeclaration.addAfter(JetPsiFactory.createNewLine(project), body);
                        }

                        String functionBody = "";
                        if (typeDescriptor.getKind() != ClassKind.TRAIT && functionDescriptor.getModality() != Modality.ABSTRACT) {
                            functionBody = "{}";
                            JetType returnType = functionDescriptor.getReturnType();
                            if (returnType == null || !KotlinBuiltIns.getInstance().isUnit(returnType)) {
                                functionBody = "{ throw UnsupportedOperationException() }";
                            }
                        }
                        JetNamedFunction functionElement = JetPsiFactory.createFunction(project, signatureString + functionBody);
                        PsiElement anchor = body.getRBrace();
                        JetNamedFunction insertedFunctionElement = (JetNamedFunction) body.addBefore(functionElement, anchor);

                        ReferenceToClassesShortening.compactReferenceToClasses(Collections.singletonList(insertedFunctionElement));
                    }
                });
         }
        }, JetBundle.message("add.function.to.type.action"), null);
    }


}
