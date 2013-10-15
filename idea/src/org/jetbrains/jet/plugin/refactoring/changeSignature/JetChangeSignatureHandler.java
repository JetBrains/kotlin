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

package org.jetbrains.jet.plugin.refactoring.changeSignature;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.changeSignature.ChangeSignatureHandler;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.plugin.refactoring.JetRefactoringBundle;

import static org.jetbrains.jet.plugin.refactoring.changeSignature.ChangeSignaturePackage.runChangeSignature;

public class JetChangeSignatureHandler implements ChangeSignatureHandler {
    @Nullable
    public static PsiElement findTargetForRefactoring(@NotNull PsiElement element) {
        if (PsiTreeUtil.getParentOfType(element, JetParameterList.class) != null) {
            return PsiTreeUtil.getParentOfType(element, JetFunction.class, JetClass.class);
        }

        JetTypeParameterList typeParameterList = PsiTreeUtil.getParentOfType(element, JetTypeParameterList.class);
        if (typeParameterList != null) {
            return PsiTreeUtil.getParentOfType(typeParameterList, JetFunction.class, JetClass.class);
        }

        PsiElement elementParent = element.getParent();
        if (elementParent instanceof JetNamedFunction && ((JetNamedFunction) elementParent).getNameIdentifier() == element) {
            return elementParent;
        }
        if (elementParent instanceof JetClass && ((JetClass) elementParent).getNameIdentifier() == element) {
            return elementParent;
        }

        JetCallElement call = PsiTreeUtil.getParentOfType(element, JetCallExpression.class, JetDelegatorToSuperCall.class);
        if (call == null) {
            return null;
        }
        JetExpression receiverExpr = call instanceof JetCallExpression ? call.getCalleeExpression() :
                                     ((JetDelegatorToSuperCall) call).getCalleeExpression().getConstructorReferenceExpression();

        if (receiverExpr instanceof JetSimpleNameExpression) {
            BindingContext bindingContext =
                    AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) element.getContainingFile()).getBindingContext();
            DeclarationDescriptor descriptor =
                    bindingContext.get(BindingContext.REFERENCE_TARGET, (JetSimpleNameExpression) receiverExpr);

            if (descriptor instanceof ClassDescriptor || descriptor instanceof FunctionDescriptor) {
                return receiverExpr;
            }
        }

        return null;
    }

    public static void invokeChangeSignature(
            @NotNull PsiElement element,
            @NotNull PsiElement context,
            @NotNull Project project,
            @Nullable Editor editor
    ) {
        BindingContext bindingContext =
                AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) element.getContainingFile()).getBindingContext();
        FunctionDescriptor functionDescriptor = findDescriptor(element, project, editor, bindingContext);
        if (functionDescriptor == null) {
            return;
        }
        runChangeSignature(project, functionDescriptor, emptyConfiguration(), bindingContext, context, null, false);
    }

    @TestOnly
    public static JetChangeSignatureConfiguration getConfiguration() {
        return emptyConfiguration();
    }

    private static JetChangeSignatureConfiguration emptyConfiguration() {
        return new JetChangeSignatureConfiguration() {
            @Override
            public void configure(
                    @NotNull JetChangeSignatureData changeSignatureData, @NotNull BindingContext bindingContext
            ) {
                //do nothing
            }
        };
    }

    @Nullable
    @Override
    public PsiElement findTargetMember(PsiFile file, Editor editor) {
        return findTargetMember(file.findElementAt(editor.getCaretModel().getOffset()));
    }

    @Nullable
    @Override
    public PsiElement findTargetMember(PsiElement element) {
        return findTargetForRefactoring(element);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file, DataContext dataContext) {
        editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
        PsiElement element = findTargetMember(file, editor);
        if (element == null) {
            element = LangDataKeys.PSI_ELEMENT.getData(dataContext);
        }

        PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
        if (element != null && elementAtCaret != null) {
            invokeChangeSignature(element, elementAtCaret, project, editor);
        }
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, @Nullable DataContext dataContext) {
        if (elements.length != 1) return;
        Editor editor = dataContext != null ? PlatformDataKeys.EDITOR.getData(dataContext) : null;
        invokeChangeSignature(elements[0], elements[0], project, editor);
    }

    @Nullable
    @Override
    public String getTargetNotFoundMessage() {
        return JetRefactoringBundle.message("error.wrong.caret.position.function.or.constructor.name");
    }

    @Nullable
    public static FunctionDescriptor findDescriptor(
            @NotNull PsiElement element,
            @NotNull Project project,
            @Nullable Editor editor,
            BindingContext bindingContext
    ) {
        if (!CommonRefactoringUtil.checkReadOnlyStatus(project, element)) return null;

        DeclarationDescriptor descriptor;
        if (element instanceof JetSimpleNameExpression) {
            descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, (JetReferenceExpression) element);
        } else {
            descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
        }
        if (descriptor instanceof ClassDescriptor) {
            descriptor = ((ClassDescriptor) descriptor).getUnsubstitutedPrimaryConstructor();
        }
        if (descriptor instanceof FunctionDescriptor) {
            for (ValueParameterDescriptor parameter : ((FunctionDescriptor) descriptor).getValueParameters()) {
                if (parameter.getVarargElementType() != null) {
                    String message = JetRefactoringBundle.message("error.cant.refactor.vararg.functions");
                    CommonRefactoringUtil.showErrorHint(project, editor, message,
                                                        REFACTORING_NAME,
                                                        HelpID.CHANGE_SIGNATURE);
                    return null;
                }
            }
            if (((FunctionDescriptor) descriptor).getKind() == SYNTHESIZED) {
                String message = JetRefactoringBundle.message("cannot.refactor.synthesized.function", descriptor.getName());
                CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.CHANGE_SIGNATURE);
                return null;
            }


            return (FunctionDescriptor) descriptor;
        }
        else {
            String message = RefactoringBundle.getCannotRefactorMessage(JetRefactoringBundle.message(
                    "error.wrong.caret.position.function.or.constructor.name"));
            CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.CHANGE_SIGNATURE);
            return null;
        }
    }
}
