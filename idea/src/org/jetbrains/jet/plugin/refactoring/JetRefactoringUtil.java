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

package org.jetbrains.jet.plugin.refactoring;

import com.intellij.codeInsight.unwrap.ScopeHighlighter;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupAdapter;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiFormatUtilBase;
import com.intellij.ui.components.JBList;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.java.JetClsMethod;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.NamespaceType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetKeywordToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.codeInsight.CodeInsightUtils;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * User: Alefas
 * Date: 25.01.12
 */
public class JetRefactoringUtil {

    private JetRefactoringUtil() {
    }

    public static JetKeywordToken getVisibilityToken(Visibility visibility) {
        if (visibility == Visibilities.PUBLIC) {
            return JetTokens.PUBLIC_KEYWORD;
        }
        else if (visibility == Visibilities.PROTECTED) {
            return JetTokens.PROTECTED_KEYWORD;
        }
        else if (visibility == Visibilities.INTERNAL) {
            return JetTokens.INTERNAL_KEYWORD;
        }
        else if (visibility == Visibilities.PRIVATE) {
            return JetTokens.PRIVATE_KEYWORD;
        }

        throw new IllegalArgumentException("Unexpected visibility '" + visibility + "'");
    }

    @NotNull
    public static String wrapOrSkip(@NotNull String s, boolean inCode) {
        return inCode ? "<code>" + s + "</code>" : s;
    }

    @NotNull
    public static String formatClassDescriptor(@NotNull DeclarationDescriptor classDescriptor) {
        return DescriptorRenderer.SOURCE_CODE_SHORT_NAMES_IN_TYPES.render(classDescriptor);
    }

    @NotNull
    public static String formatPsiClass(
            @NotNull PsiClass psiClass,
            boolean markAsJava,
            boolean inCode
    ) {
        String description;

        String kind = psiClass.isInterface() ? "interface " : "class ";
        description = kind + PsiFormatUtil.formatClass(
                psiClass,
                PsiFormatUtilBase.SHOW_CONTAINING_CLASS
                | PsiFormatUtilBase.SHOW_NAME
                | PsiFormatUtilBase.SHOW_PARAMETERS
                | PsiFormatUtilBase.SHOW_TYPE
        );
        description = wrapOrSkip(description, inCode);

        return markAsJava ? "[Java] " + description : description;
    }

    @Nullable
    public static Collection<? extends PsiElement> checkSuperMethods(
            @NotNull JetDeclaration declaration, @Nullable Collection<PsiElement> ignore, @NotNull String actionStringKey
    ) {
        final BindingContext bindingContext =
                AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) declaration.getContainingFile()).getBindingContext();

        DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);
        if (!(declarationDescriptor instanceof CallableMemberDescriptor)) return null;

        CallableMemberDescriptor callableDescriptor = (CallableMemberDescriptor) declarationDescriptor;
        Set<? extends CallableMemberDescriptor> overridenDescriptors = callableDescriptor.getOverriddenDescriptors();

        Collection<? extends PsiElement> superMethods = ContainerUtil.map(
                overridenDescriptors,
                new Function<CallableMemberDescriptor, PsiElement>() {
                    @Override
                    public PsiElement fun(CallableMemberDescriptor descriptor) {
                        return BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor);
                    }
                }
        );
        if (ignore != null) {
            superMethods.removeAll(ignore);
        }

        if (superMethods.isEmpty()) return Collections.singletonList(declaration);

        java.util.List<String> superClasses = getClassDescriptions(bindingContext, superMethods);
        return askUserForMethodsToSearch(declaration, callableDescriptor, superMethods, superClasses, actionStringKey);
    }

    @NotNull
    private static Collection<? extends PsiElement> askUserForMethodsToSearch(
            @NotNull JetDeclaration declaration,
            @NotNull CallableMemberDescriptor callableDescriptor,
            @NotNull Collection<? extends PsiElement> superMethods,
            @NotNull List<String> superClasses,
            @NotNull String actionStringKey
    ) {
        String superClassesStr = "\n" + StringUtil.join(superClasses, "");
        String message = JetBundle.message(
                "x.overrides.y.in.class.list",
                DescriptorRenderer.COMPACT.render(callableDescriptor),
                DescriptorRenderer.SOURCE_CODE_SHORT_NAMES_IN_TYPES.render(callableDescriptor.getContainingDeclaration()),
                superClassesStr,
                JetBundle.message(actionStringKey)
        );

        int exitCode = Messages.showYesNoCancelDialog(
                declaration.getProject(), message, IdeBundle.message("title.warning"), Messages.getQuestionIcon()
        );
        switch (exitCode) {
            case Messages.YES:
                return superMethods;
            case Messages.NO:
                return Collections.singletonList(declaration);
            default:
                return Collections.emptyList();
        }
    }

    @NotNull
    private static List<String> getClassDescriptions(
            @NotNull final BindingContext bindingContext, @NotNull Collection<? extends PsiElement> superMethods
    ) {
        return ContainerUtil.map(
                superMethods,
                new Function<PsiElement, String>() {
                    @Override
                    public String fun(PsiElement element) {
                        String description;

                        if (element instanceof JetNamedFunction || element instanceof JetProperty) {
                            DeclarationDescriptor descriptor =
                                    bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
                            assert descriptor != null;

                            DeclarationDescriptor containingDescriptor = descriptor.getContainingDeclaration();
                            assert containingDescriptor != null;

                            description = formatClassDescriptor(containingDescriptor);
                        }
                        else {
                            assert element instanceof PsiMethod;

                            PsiClass psiClass = ((PsiMethod) element).getContainingClass();
                            assert psiClass != null;

                            description = formatPsiClass(psiClass, true, false);
                        }

                        return "    " + description + "\n";
                    }
                }
        );
    }

    @NotNull
    public static String formatClass(
            @NotNull DeclarationDescriptor classDescriptor,
            @NotNull BindingContext bindingContext,
            boolean inCode
    ) {
        PsiElement element = BindingContextUtils.descriptorToDeclaration(bindingContext, classDescriptor);
        if (element instanceof PsiClass) {
            return formatPsiClass((PsiClass) element, false, inCode);
        }

        return wrapOrSkip(formatClassDescriptor(classDescriptor), inCode);
    }

    @NotNull
    public static String formatFunction(
            @NotNull DeclarationDescriptor functionDescriptor,
            @NotNull BindingContext bindingContext,
            boolean inCode
    ) {
        PsiElement element = BindingContextUtils.descriptorToDeclaration(bindingContext, functionDescriptor);
        if (element instanceof PsiMethod) {
            return formatPsiMethod((PsiMethod) element, false, inCode);
        }

        return wrapOrSkip(formatFunctionDescriptor(functionDescriptor), inCode);
    }

    @NotNull
    private static String formatFunctionDescriptor(@NotNull DeclarationDescriptor functionDescriptor) {
        return DescriptorRenderer.COMPACT.render(functionDescriptor);
    }

    @NotNull
    public static String formatPsiMethod(
            @NotNull PsiMethod psiMethod,
            boolean showContainingClass,
            boolean inCode) {
        int options = PsiFormatUtilBase.SHOW_NAME | PsiFormatUtilBase.SHOW_PARAMETERS | PsiFormatUtilBase.SHOW_TYPE;
        if (showContainingClass) {
            //noinspection ConstantConditions
            options |= PsiFormatUtilBase.SHOW_CONTAINING_CLASS;
        }

        String description = PsiFormatUtil.formatMethod(psiMethod, PsiSubstitutor.EMPTY, options, PsiFormatUtilBase.SHOW_TYPE);
        description = wrapOrSkip(description, inCode);

        return "[Java] " + description;
    }

    @NotNull
    public static String formatJavaOrLightMethod(@NotNull PsiMethod method) {
        if (method instanceof JetClsMethod) {
            JetDeclaration declaration = ((JetClsMethod) method).getOrigin();
            BindingContext bindingContext =
                    AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) declaration.getContainingFile()).getBindingContext();
            DeclarationDescriptor descriptor =
                    bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);
            if (descriptor != null) return formatFunctionDescriptor(descriptor);
        }
        return formatPsiMethod(method, false, false);
    }

    @NotNull
    public static String formatClass(@NotNull JetClassOrObject classOrObject) {
        BindingContext bindingContext =
                AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) classOrObject.getContainingFile()).getBindingContext();
        DeclarationDescriptor descriptor =
                bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, classOrObject);

        if (descriptor instanceof ClassDescriptor) return formatClassDescriptor(descriptor);
        return "class " + classOrObject.getName();
    }

    public interface SelectExpressionCallback {
        void run(@Nullable JetExpression expression);
    }

    public static void selectExpression(@NotNull Editor editor,
                                        @NotNull PsiFile file,
                                        @NotNull SelectExpressionCallback callback) throws IntroduceRefactoringException {
        if (editor.getSelectionModel().hasSelection()) {
            int selectionStart = editor.getSelectionModel().getSelectionStart();
            int selectionEnd = editor.getSelectionModel().getSelectionEnd();
            String text = file.getText();
            while (selectionStart < selectionEnd && Character.isSpaceChar(text.charAt(selectionStart))) ++selectionStart;
            while (selectionStart < selectionEnd && Character.isSpaceChar(text.charAt(selectionEnd - 1))) --selectionEnd;
            callback.run(findExpression(file, selectionStart, selectionEnd));
        }
        else {
            int offset = editor.getCaretModel().getOffset();
            smartSelectExpression(editor, file, offset, callback);
        }
    }

    private static void smartSelectExpression(@NotNull Editor editor, @NotNull PsiFile file, int offset,
                                             @NotNull final SelectExpressionCallback callback)
            throws IntroduceRefactoringException {
        if (offset < 0) throw new IntroduceRefactoringException(JetRefactoringBundle.message("cannot.refactor.not.expression"));
        PsiElement element = file.findElementAt(offset);
        if (element == null) throw new IntroduceRefactoringException(JetRefactoringBundle.message("cannot.refactor.not.expression"));
        if (element instanceof PsiWhiteSpace) {
            smartSelectExpression(editor, file, offset - 1, callback);
            return;
        }
        ArrayList<JetExpression> expressions = new ArrayList<JetExpression>();
        while (element != null && !(element instanceof JetBlockExpression && !(element.getParent() instanceof JetFunctionLiteral)) &&
               !(element instanceof JetNamedFunction)
               && !(element instanceof JetClassBody)) {
            if (element instanceof JetExpression && !(element instanceof JetStatementExpression)) {
                boolean addExpression = true;
                if (element.getParent() instanceof JetQualifiedExpression) {
                    JetQualifiedExpression qualifiedExpression = (JetQualifiedExpression) element.getParent();
                    if (qualifiedExpression.getReceiverExpression() != element) {
                        addExpression = false;
                    }
                }
                else if (element.getParent() instanceof JetCallElement) {
                    addExpression = false;
                }
                else if (element.getParent() instanceof JetOperationExpression) {
                    JetOperationExpression operationExpression = (JetOperationExpression) element.getParent();
                    if (operationExpression.getOperationReference() == element) {
                        addExpression = false;
                    }
                }
                if (addExpression) {
                    JetExpression expression = (JetExpression)element;
                    BindingContext bindingContext = AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) expression.getContainingFile()).getBindingContext();
                    JetType expressionType = bindingContext.get(BindingContext.EXPRESSION_TYPE, expression);
                    if (expressionType == null || !(expressionType instanceof NamespaceType) &&
                                                  !JetTypeChecker.INSTANCE.equalTypes(KotlinBuiltIns.
                                                          getInstance().getUnitType(), expressionType)) {
                        expressions.add(expression);
                    }
                }
            }
            element = element.getParent();
        }
        if (expressions.size() == 0) throw new IntroduceRefactoringException(JetRefactoringBundle.message("cannot.refactor.not.expression"));

        final DefaultListModel model = new DefaultListModel();
        for (JetExpression expression : expressions) {
            model.addElement(expression);
        }

        final ScopeHighlighter highlighter = new ScopeHighlighter(editor);

        final JList list = new JBList(model);
        
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component rendererComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                JetExpression element = (JetExpression) value;
                if (element.isValid()) {
                    setText(getExpressionShortText(element));
                }
                return rendererComponent;
            }
        });

        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                highlighter.dropHighlight();
                int selectedIndex = list.getSelectedIndex();
                if (selectedIndex < 0) return;
                JetExpression expression = (JetExpression) model.get(selectedIndex);
                ArrayList<PsiElement> toExtract = new ArrayList<PsiElement>();
                toExtract.add(expression);
                highlighter.highlight(expression, toExtract);
            }
        });

        JBPopupFactory.getInstance().createListPopupBuilder(list).
                setTitle(JetRefactoringBundle.message("expressions.title")).setMovable(false).setResizable(false).
                setRequestFocus(true).setItemChoosenCallback(new Runnable() {
            @Override
            public void run() {
                callback.run((JetExpression) list.getSelectedValue());
            }
        }).addListener(new JBPopupAdapter() {
            @Override
            public void onClosed(LightweightWindowEvent event) {
                highlighter.dropHighlight();
            }
        }).createPopup().showInBestPositionFor(editor);
        
    }

    public static String getExpressionShortText(@NotNull JetElement element) { //todo: write appropriate implementation
        String expressionText = element.getText();
        if (expressionText.length() > 20) {
            expressionText = expressionText.substring(0, 17) + "...";
        }
        return expressionText;
    }

    @NotNull
    private static JetExpression findExpression(@NotNull PsiFile file, int startOffset, int endOffset) throws IntroduceRefactoringException {
        JetExpression element = CodeInsightUtils.findExpression(file, startOffset, endOffset);
        if (element == null) {
            //todo: if it's infix expression => add (), then commit document then return new created expression
            throw new IntroduceRefactoringException(JetRefactoringBundle.message("cannot.refactor.not.expression"));
        }
        return element;
    }

    public static class IntroduceRefactoringException extends Exception {
        private String myMessage;

        public IntroduceRefactoringException(String message) {
            myMessage = message;
        }

        public String getMessage() {
            return myMessage;
        }
    }

}
