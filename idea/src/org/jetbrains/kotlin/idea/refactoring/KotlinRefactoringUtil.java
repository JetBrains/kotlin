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

package org.jetbrains.kotlin.idea.refactoring;

import com.intellij.codeInsight.unwrap.ScopeHighlighter;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupAdapter;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiFormatUtilBase;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.components.JBList;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.KtLightMethod;
import org.jetbrains.kotlin.asJava.LightClassUtilsKt;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor;
import org.jetbrains.kotlin.idea.KotlinBundle;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils;
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde;
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers;
import org.jetbrains.kotlin.idea.util.string.StringUtilKt;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;
import org.jetbrains.kotlin.types.KotlinType;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.*;
import java.util.List;

public class KotlinRefactoringUtil {
    private KotlinRefactoringUtil() {
    }

    @NotNull
    public static String wrapOrSkip(@NotNull String s, boolean inCode) {
        return inCode ? "<code>" + s + "</code>" : s;
    }

    @NotNull
    public static String formatClassDescriptor(@NotNull DeclarationDescriptor classDescriptor) {
        return IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.render(classDescriptor);
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

    @NotNull
    public static List<? extends PsiElement> checkSuperMethods(
            @NotNull KtDeclaration declaration,
            @Nullable Collection<PsiElement> ignore,
            @NotNull String actionStringKey
    ) {
        BindingContext bindingContext = ResolutionUtils.analyze(declaration, BodyResolveMode.FULL);

        CallableDescriptor declarationDescriptor =
                (CallableDescriptor)bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);

        if (declarationDescriptor == null || declarationDescriptor instanceof LocalVariableDescriptor) {
            return Collections.singletonList(declaration);
        }

        Project project = declaration.getProject();
        Map<PsiElement, CallableDescriptor> overriddenElementsToDescriptor = new HashMap<PsiElement, CallableDescriptor>();
        for (CallableDescriptor overriddenDescriptor : DescriptorUtils.getAllOverriddenDescriptors(declarationDescriptor)) {
            PsiElement overriddenDeclaration = DescriptorToSourceUtilsIde.INSTANCE$.getAnyDeclaration(project, overriddenDescriptor);
            if (PsiTreeUtil.instanceOf(overriddenDeclaration, KtNamedFunction.class, KtProperty.class, PsiMethod.class)) {
                overriddenElementsToDescriptor.put(overriddenDeclaration, overriddenDescriptor);
            }
        }
        if (ignore != null) {
            overriddenElementsToDescriptor.keySet().removeAll(ignore);
        }

        if (overriddenElementsToDescriptor.isEmpty()) return Collections.singletonList(declaration);

        List<String> superClasses = getClassDescriptions(overriddenElementsToDescriptor);
        return askUserForMethodsToSearch(declaration, declarationDescriptor, overriddenElementsToDescriptor, superClasses, actionStringKey);
    }

    @NotNull
    private static List<? extends PsiElement> askUserForMethodsToSearch(
            @NotNull KtDeclaration declaration,
            @NotNull CallableDescriptor declarationDescriptor,
            @NotNull Map<PsiElement, CallableDescriptor> overriddenElementsToDescriptor,
            @NotNull List<String> superClasses,
            @NotNull String actionStringKey
    ) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return ContainerUtil.newArrayList(overriddenElementsToDescriptor.keySet());
        }

        String superClassesStr = "\n" + StringUtil.join(superClasses, "");
        String message = KotlinBundle.message(
                "x.overrides.y.in.class.list",
                DescriptorRenderer.COMPACT_WITH_SHORT_TYPES.render(declarationDescriptor),
                IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.render(declarationDescriptor.getContainingDeclaration()),
                superClassesStr,
                KotlinBundle.message(actionStringKey)
        );

        int exitCode = Messages.showYesNoCancelDialog(declaration.getProject(), message, IdeBundle.message("title.warning"), Messages.getQuestionIcon());
        switch (exitCode) {
            case Messages.YES:
                return ContainerUtil.newArrayList(overriddenElementsToDescriptor.keySet());
            case Messages.NO:
                return Collections.singletonList(declaration);
            default:
                return Collections.emptyList();
        }
    }

    @NotNull
    private static List<String> getClassDescriptions(@NotNull Map<PsiElement, CallableDescriptor> overriddenElementsToDescriptor) {
        return ContainerUtil.map(
                overriddenElementsToDescriptor.entrySet(),
                new Function<Map.Entry<PsiElement, CallableDescriptor>, String>() {
                    @Override
                    public String fun(Map.Entry<PsiElement, CallableDescriptor> entry) {
                        String description;

                        PsiElement element = entry.getKey();
                        CallableDescriptor descriptor = entry.getValue();
                        if (element instanceof KtNamedFunction || element instanceof KtProperty) {
                            description = formatClassDescriptor(descriptor.getContainingDeclaration());
                        }
                        else {
                            assert element instanceof PsiMethod : "Invalid element: " + element.getText();

                            PsiClass psiClass = ((PsiMethod) element).getContainingClass();
                            assert psiClass != null : "Invalid element: " + element.getText();

                            description = formatPsiClass(psiClass, true, false);
                        }

                        return "    " + description + "\n";
                    }
                }
        );
    }

    @NotNull
    public static String formatClass(@NotNull DeclarationDescriptor classDescriptor, boolean inCode) {
        PsiElement element = DescriptorToSourceUtils.descriptorToDeclaration(classDescriptor);
        if (element instanceof PsiClass) {
            return formatPsiClass((PsiClass) element, false, inCode);
        }

        return wrapOrSkip(formatClassDescriptor(classDescriptor), inCode);
    }

    @NotNull
    public static String formatFunction(@NotNull DeclarationDescriptor functionDescriptor, boolean inCode) {
        PsiElement element = DescriptorToSourceUtils.descriptorToDeclaration(functionDescriptor);
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
        PsiElement originalDeclaration = LightClassUtilsKt.getUnwrapped(method);
        if (originalDeclaration instanceof KtDeclaration) {
            KtDeclaration ktDeclaration = (KtDeclaration) originalDeclaration;
            BindingContext bindingContext = ResolutionUtils.analyze(ktDeclaration, BodyResolveMode.FULL);
            DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, ktDeclaration);

            if (descriptor != null) return formatFunctionDescriptor(descriptor);
        }
        return formatPsiMethod(method, false, false);
    }

    @NotNull
    public static String formatClass(@NotNull KtClassOrObject classOrObject) {
        BindingContext bindingContext = ResolutionUtils.analyze(classOrObject, BodyResolveMode.FULL);
        DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, classOrObject);

        if (descriptor instanceof ClassDescriptor) return formatClassDescriptor(descriptor);
        return "class " + classOrObject.getName();
    }

    @Nullable
    public static Collection<? extends PsiElement> checkParametersInMethodHierarchy(@NotNull PsiParameter parameter) {
        PsiMethod method = (PsiMethod)parameter.getDeclarationScope();

        Set<PsiElement> parametersToDelete = collectParametersHierarchy(method, parameter);
        if (parametersToDelete.size() > 1) {
            if (ApplicationManager.getApplication().isUnitTestMode()) {
                return parametersToDelete;
            }

            String message =
                    KotlinBundle.message("delete.param.in.method.hierarchy", formatJavaOrLightMethod(method));
            int exitCode = Messages.showOkCancelDialog(
                    parameter.getProject(), message, IdeBundle.message("title.warning"), Messages.getQuestionIcon()
            );
            if (exitCode == Messages.OK) {
                return parametersToDelete;
            }
            else {
                return null;
            }
        }

        return parametersToDelete;
    }

    // TODO: generalize breadth-first search
    @NotNull
    private static Set<PsiElement> collectParametersHierarchy(@NotNull PsiMethod method, @NotNull PsiParameter parameter) {
        Deque<PsiMethod> queue = new ArrayDeque<PsiMethod>();
        Set<PsiMethod> visited = new HashSet<PsiMethod>();
        Set<PsiElement> parametersToDelete = new HashSet<PsiElement>();

        queue.add(method);
        while (!queue.isEmpty()) {
            PsiMethod currentMethod = queue.poll();

            visited.add(currentMethod);
            addParameter(currentMethod, parametersToDelete, parameter);

            for (PsiMethod superMethod : currentMethod.findSuperMethods(true)) {
                if (!visited.contains(superMethod)) {
                    queue.offer(superMethod);
                }
            }
            for (PsiMethod overrider : OverridingMethodsSearch.search(currentMethod)) {
                if (!visited.contains(overrider)) {
                    queue.offer(overrider);
                }
            }
        }
        return parametersToDelete;
    }

    private static void addParameter(@NotNull PsiMethod method, @NotNull Set<PsiElement> result, @NotNull PsiParameter parameter) {
        int parameterIndex = KtPsiUtilKt.parameterIndex(LightClassUtilsKt.getUnwrapped(parameter));

        if (method instanceof KtLightMethod) {
            KtDeclaration declaration = ((KtLightMethod) method).getOrigin();
            if (declaration instanceof KtFunction) {
                result.add(((KtFunction) declaration).getValueParameters().get(parameterIndex));
            }
        }
        else {
            result.add(method.getParameterList().getParameters()[parameterIndex]);
        }
    }

    public interface SelectExpressionCallback {
        void run(@Nullable KtExpression expression);
    }

    public static void selectExpression(
            @NotNull Editor editor,
            @NotNull PsiFile file,
            @NotNull SelectExpressionCallback callback) throws IntroduceRefactoringException {
        selectExpression(editor, file, true, callback);
    }

    public static void selectExpression(@NotNull Editor editor,
                                        @NotNull PsiFile file,
                                        boolean failOnEmptySuggestion,
                                        @NotNull SelectExpressionCallback callback) throws IntroduceRefactoringException {
        if (editor.getSelectionModel().hasSelection()) {
            int selectionStart = editor.getSelectionModel().getSelectionStart();
            int selectionEnd = editor.getSelectionModel().getSelectionEnd();
            String text = file.getText();
            while (selectionStart < selectionEnd && Character.isSpaceChar(text.charAt(selectionStart))) ++selectionStart;
            while (selectionStart < selectionEnd && Character.isSpaceChar(text.charAt(selectionEnd - 1))) --selectionEnd;
            callback.run(findExpression(file, selectionStart, selectionEnd, failOnEmptySuggestion));
        }
        else {
            int offset = editor.getCaretModel().getOffset();
            smartSelectExpression(editor, file, offset, failOnEmptySuggestion, callback);
        }
    }

    public static List<KtExpression> getSmartSelectSuggestions(
            @NotNull PsiFile file,
            int offset
    ) throws IntroduceRefactoringException {
        if (offset < 0) {
            return new ArrayList<KtExpression>();
        }

        PsiElement element = file.findElementAt(offset);
        if (element == null) {
            return new ArrayList<KtExpression>();
        }
        if (element instanceof PsiWhiteSpace) {
            return getSmartSelectSuggestions(file, offset - 1);
        }

        List<KtExpression> expressions = new ArrayList<KtExpression>();
        while (element != null && !(element instanceof KtBlockExpression && !(element.getParent() instanceof KtFunctionLiteral)) &&
               !(element instanceof KtNamedFunction)
               && !(element instanceof KtClassBody)) {
            if (element instanceof KtExpression && !(element instanceof KtStatementExpression)) {
                boolean addExpression = true;

                if (KtPsiUtil.isLabelIdentifierExpression(element)) {
                    addExpression = false;
                }
                else if (element.getParent() instanceof KtQualifiedExpression) {
                    KtQualifiedExpression qualifiedExpression = (KtQualifiedExpression) element.getParent();
                    if (qualifiedExpression.getReceiverExpression() != element) {
                        addExpression = false;
                    }
                }
                else if (element.getParent() instanceof KtCallElement
                         || element.getParent() instanceof KtThisExpression
                         || PsiTreeUtil.getParentOfType(element, KtSuperExpression.class) != null) {
                    addExpression = false;
                }
                else if (element.getParent() instanceof KtOperationExpression) {
                    KtOperationExpression operationExpression = (KtOperationExpression) element.getParent();
                    if (operationExpression.getOperationReference() == element) {
                        addExpression = false;
                    }
                }
                if (addExpression) {
                    KtExpression expression = (KtExpression)element;
                    BindingContext bindingContext = ResolutionUtils.analyze(expression, BodyResolveMode.FULL);
                    KotlinType expressionType = bindingContext.getType(expression);
                    if (expressionType == null || !KotlinBuiltIns.isUnit(expressionType)) {
                        expressions.add(expression);
                    }
                }
            }
            else if (element instanceof KtTypeElement) {
                expressions.clear();
            }
            element = element.getParent();
        }
        return expressions;
    }

    private static void smartSelectExpression(
            @NotNull Editor editor, @NotNull PsiFile file, int offset,
            boolean failOnEmptySuggestion,
            @NotNull final SelectExpressionCallback callback) throws IntroduceRefactoringException {
        List<KtExpression> expressions = getSmartSelectSuggestions(file, offset);
        if (expressions.size() == 0) {
            if (failOnEmptySuggestion) throw new IntroduceRefactoringException(
                    KotlinRefactoringBundle.message("cannot.refactor.not.expression"));
            return;
        }

        if (expressions.size() == 1 || ApplicationManager.getApplication().isUnitTestMode()) {
            callback.run(expressions.get(0));
            return;
        }

        final DefaultListModel model = new DefaultListModel();
        for (KtExpression expression : expressions) {
            model.addElement(expression);
        }

        final ScopeHighlighter highlighter = new ScopeHighlighter(editor);

        final JList list = new JBList(model);
        
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(@NotNull JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component rendererComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                KtExpression element = (KtExpression) value;
                if (element.isValid()) {
                    setText(getExpressionShortText(element));
                }
                return rendererComponent;
            }
        });

        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(@NotNull ListSelectionEvent e) {
                highlighter.dropHighlight();
                int selectedIndex = list.getSelectedIndex();
                if (selectedIndex < 0) return;
                KtExpression expression = (KtExpression) model.get(selectedIndex);
                List<PsiElement> toExtract = new ArrayList<PsiElement>();
                toExtract.add(expression);
                highlighter.highlight(expression, toExtract);
            }
        });

        JBPopupFactory.getInstance().createListPopupBuilder(list).
                setTitle(KotlinRefactoringBundle.message("expressions.title")).setMovable(false).setResizable(false).
                setRequestFocus(true).setItemChoosenCallback(new Runnable() {
            @Override
            public void run() {
                callback.run((KtExpression) list.getSelectedValue());
            }
        }).addListener(new JBPopupAdapter() {
            @Override
            public void onClosed(LightweightWindowEvent event) {
                highlighter.dropHighlight();
            }
        }).createPopup().showInBestPositionFor(editor);
        
    }

    public static String getExpressionShortText(@NotNull KtElement element) { //todo: write appropriate implementation
        return StringUtilKt.collapseSpaces(StringUtil.shortenTextWithEllipsis(element.getText(), 53, 0));
    }

    @Nullable
    private static KtExpression findExpression(
            @NotNull PsiFile file, int startOffset, int endOffset, boolean failOnNoExpression
    ) throws IntroduceRefactoringException {
        KtExpression element = CodeInsightUtils.findExpression(file, startOffset, endOffset);
        if (element == null) {
            //todo: if it's infix expression => add (), then commit document then return new created expression

            if (failOnNoExpression) {
                throw new IntroduceRefactoringException(KotlinRefactoringBundle.message("cannot.refactor.not.expression"));
            }
            return null;
        }
        return element;
    }

    public static class IntroduceRefactoringException extends Exception {
        private final String myMessage;

        public IntroduceRefactoringException(String message) {
            myMessage = message;
        }

        @Override
        public String getMessage() {
            return myMessage;
        }
    }

}
