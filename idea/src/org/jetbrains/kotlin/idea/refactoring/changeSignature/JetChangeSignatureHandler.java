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

package org.jetbrains.kotlin.idea.refactoring.changeSignature;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.HelpID;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.changeSignature.ChangeSignatureHandler;
import com.intellij.refactoring.changeSignature.ChangeSignatureUtil;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.asJava.AsJavaPackage;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils;
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde;
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringBundle;
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.calls.tasks.TasksPackage;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;

import java.util.Collection;

import static org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED;
import static org.jetbrains.kotlin.idea.refactoring.changeSignature.ChangeSignaturePackage.runChangeSignature;

public class JetChangeSignatureHandler implements ChangeSignatureHandler {
    @Nullable
    public static PsiElement findTargetForRefactoring(@NotNull PsiElement element) {
        PsiElement elementParent = element.getParent();
        if ((elementParent instanceof JetNamedFunction || elementParent instanceof JetClass || elementParent instanceof JetProperty)
            && ((JetNamedDeclaration) elementParent).getNameIdentifier() == element) return elementParent;

        if (elementParent instanceof JetParameter) {
            JetParameter parameter = (JetParameter) elementParent;
            JetPrimaryConstructor primaryConstructor = PsiTreeUtil.getParentOfType(parameter, JetPrimaryConstructor.class);
            if (parameter.hasValOrVar()
                && (parameter.getNameIdentifier() == element || parameter.getValOrVarKeyword() == element)
                && primaryConstructor != null
                && primaryConstructor.getValueParameterList() == parameter.getParent()) return parameter;
        }

        if (elementParent instanceof JetSecondaryConstructor &&
            ((JetSecondaryConstructor) elementParent).getConstructorKeyword() == element) return elementParent;

        if (PsiTreeUtil.getParentOfType(element, JetParameterList.class) != null) {
            return PsiTreeUtil.getParentOfType(element, JetFunction.class, JetProperty.class, JetClass.class);
        }

        JetTypeParameterList typeParameterList = PsiTreeUtil.getParentOfType(element, JetTypeParameterList.class);
        if (typeParameterList != null) {
            return PsiTreeUtil.getParentOfType(typeParameterList, JetFunction.class, JetProperty.class, JetClass.class);
        }

        JetExpression calleeExpr;
        JetCallElement call = PsiTreeUtil.getParentOfType(element,
                                                          JetCallExpression.class,
                                                          JetDelegatorToSuperCall.class,
                                                          JetConstructorDelegationCall.class);
        if (call != null) {
            calleeExpr = call.getCalleeExpression();
        }
        else {
            calleeExpr = PsiTreeUtil.getParentOfType(element, JetSimpleNameExpression.class);
        }

        if (calleeExpr instanceof JetConstructorCalleeExpression) {
            calleeExpr = ((JetConstructorCalleeExpression) calleeExpr).getConstructorReferenceExpression();
        }
        if (calleeExpr instanceof JetSimpleNameExpression || calleeExpr instanceof JetConstructorDelegationReferenceExpression) {
            JetElement jetElement = PsiTreeUtil.getParentOfType(element, JetElement.class);
            if (jetElement == null) return null;

            BindingContext bindingContext = ResolvePackage.analyze(jetElement, BodyResolveMode.FULL);
            DeclarationDescriptor descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, (JetReferenceExpression) calleeExpr);

            if (descriptor instanceof ClassDescriptor || descriptor instanceof CallableDescriptor) return calleeExpr;
        }

        return null;
    }

    public static void invokeChangeSignature(
            @NotNull JetElement element,
            @NotNull PsiElement context,
            @NotNull Project project,
            @Nullable Editor editor
    ) {
        BindingContext bindingContext = ResolvePackage.analyze(element, BodyResolveMode.FULL);

        CallableDescriptor callableDescriptor = findDescriptor(element, project, editor, bindingContext);
        if (callableDescriptor == null) {
            return;
        }

        if (callableDescriptor instanceof JavaCallableMemberDescriptor) {
            PsiElement declaration = DescriptorToSourceUtilsIde.INSTANCE$.getAnyDeclaration(project, callableDescriptor);
            assert declaration instanceof PsiMethod : "PsiMethod expected: " + callableDescriptor;
            ChangeSignatureUtil.invokeChangeSignatureOn((PsiMethod) declaration, project);
            return;
        }

        if (TasksPackage.isDynamic(callableDescriptor)) {
            if (editor != null) {
                CodeInsightUtils.showErrorHint(
                        project,
                        editor,
                        "Change signature is not applicable to dynamically invoked functions",
                        "Change Signature",
                        null
                );
            }
            return;
        }

        runChangeSignature(project, callableDescriptor, emptyConfiguration(), bindingContext, context, null);
    }

    @TestOnly
    public static JetChangeSignatureConfiguration getConfiguration() {
        return emptyConfiguration();
    }

    private static JetChangeSignatureConfiguration emptyConfiguration() {
        return new JetChangeSignatureConfiguration() {
            @NotNull
            @Override
            public JetMethodDescriptor configure(@NotNull JetMethodDescriptor originalDescriptor, @NotNull BindingContext bindingContext) {
                //do nothing
                return originalDescriptor;
            }

            @Override
            public boolean performSilently(@NotNull Collection<? extends PsiElement> elements) {
                return false;
            }

            @Override
            public boolean forcePerformForSelectedFunctionOnly() {
                return false;
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
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file, DataContext dataContext) {
        editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
        PsiElement element = findTargetMember(file, editor);
        if (element == null) {
            element = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
        }

        PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
        if (element != null && elementAtCaret != null) {
            assert element instanceof JetElement : "This handler must be invoked for elements of JetLanguage : " + element.getText();

            invokeChangeSignature((JetElement) element, elementAtCaret, project, editor);
        }
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, @Nullable DataContext dataContext) {
        if (elements.length != 1) return;
        Editor editor = dataContext != null ? CommonDataKeys.EDITOR.getData(dataContext) : null;

        PsiElement element = AsJavaPackage.getUnwrapped(elements[0]);
        assert element instanceof JetElement : "This handler must be invoked for elements of JetLanguage : " + element.getText();

        invokeChangeSignature((JetElement) element, element, project, editor);
    }

    @Nullable
    @Override
    public String getTargetNotFoundMessage() {
        return JetRefactoringBundle.message("error.wrong.caret.position.function.or.constructor.name");
    }

    @Nullable
    public static CallableDescriptor findDescriptor(
            @NotNull PsiElement element,
            @NotNull Project project,
            @Nullable Editor editor,
            BindingContext bindingContext
    ) {
        if (!CommonRefactoringUtil.checkReadOnlyStatus(project, element)) return null;

        DeclarationDescriptor descriptor;
        if (element instanceof JetReferenceExpression) {
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
        else if (descriptor instanceof PropertyDescriptor || descriptor instanceof ValueParameterDescriptor) {
            return (CallableDescriptor) descriptor;
        }
        else {
            String message = RefactoringBundle.getCannotRefactorMessage(JetRefactoringBundle.message(
                    "error.wrong.caret.position.function.or.constructor.name"));
            CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, HelpID.CHANGE_SIGNATURE);
            return null;
        }
    }
}
