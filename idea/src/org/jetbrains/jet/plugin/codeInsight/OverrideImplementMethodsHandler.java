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

package org.jetbrains.jet.plugin.codeInsight;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.ide.util.MemberChooser;
import com.intellij.lang.LanguageCodeInsightActionHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class OverrideImplementMethodsHandler implements LanguageCodeInsightActionHandler {

    public static List<DescriptorClassMember> membersFromDescriptors(
            JetFile file, Iterable<CallableMemberDescriptor> missingImplementations,
            BindingContext bindingContext
    ) {
        List<DescriptorClassMember> members = new ArrayList<DescriptorClassMember>();
        for (CallableMemberDescriptor memberDescriptor : missingImplementations) {

            PsiElement declaration = DescriptorToDeclarationUtil.getDeclaration(file, memberDescriptor, bindingContext);
            assert declaration != null : "Can not find declaration for descriptor " + memberDescriptor;
            DescriptorClassMember member = new DescriptorClassMember(declaration, memberDescriptor);
            members.add(member);
        }
        return members;
    }

    public static void generateMethods(
            Editor editor,
            JetClassOrObject classOrObject,
            List<DescriptorClassMember> selectedElements
    ) {
        JetClassBody body = classOrObject.getBody();
        if (body == null) {
            Project project = classOrObject.getProject();
            classOrObject.add(JetPsiFactory.createWhiteSpace(project));
            body = (JetClassBody) classOrObject.add(JetPsiFactory.createEmptyClassBody(project));
        }

        PsiElement afterAnchor = findInsertAfterAnchor(editor, body);

        if (afterAnchor == null) {
            return;
        }

        List<JetElement> elementsToCompact = new ArrayList<JetElement>();
        final JetFile file = (JetFile) classOrObject.getContainingFile();
        for (JetElement element : generateOverridingMembers(selectedElements, file)) {
            PsiElement added = body.addAfter(element, afterAnchor);
            afterAnchor = added;
            elementsToCompact.add((JetElement) added);
        }

        ReferenceToClassesShortening.compactReferenceToClasses(elementsToCompact);
    }

    @Nullable
    private static PsiElement findInsertAfterAnchor(Editor editor, final JetClassBody body) {
        PsiElement afterAnchor = body.getLBrace();
        if (afterAnchor == null) {
            return null;
        }

        int offset = editor.getCaretModel().getOffset();
        PsiElement offsetCursorElement = PsiTreeUtil.findFirstParent(body.getContainingFile().findElementAt(offset),
                                                                     new Condition<PsiElement>() {
                                                                         @Override
                                                                         public boolean value(PsiElement element) {
                                                                             return element.getParent() == body;
                                                                         }
                                                                     });

        if (offsetCursorElement != null && offsetCursorElement != body.getRBrace()) {
            afterAnchor = offsetCursorElement;
        }

        return afterAnchor;
    }

    private static List<JetElement> generateOverridingMembers(List<DescriptorClassMember> selectedElements, JetFile file) {
        List<JetElement> overridingMembers = new ArrayList<JetElement>();
        for (DescriptorClassMember selectedElement : selectedElements) {
            final DeclarationDescriptor descriptor = selectedElement.getDescriptor();
            if (descriptor instanceof SimpleFunctionDescriptor) {
                overridingMembers.add(overrideFunction(file.getProject(), (SimpleFunctionDescriptor) descriptor));
            }
            else if (descriptor instanceof PropertyDescriptor) {
                overridingMembers.add(overrideProperty(file.getProject(), (PropertyDescriptor) descriptor));
            }
        }
        return overridingMembers;
    }

    private static JetElement overrideProperty(Project project, PropertyDescriptor descriptor) {
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append(displayableVisibility(descriptor)).append("override ");
        if (descriptor.isVar()) {
            bodyBuilder.append("var ");
        }
        else {
            bodyBuilder.append("val ");
        }

        addReceiverParameter(descriptor, bodyBuilder);

        bodyBuilder.append(descriptor.getName()).append(" : ").append(
                DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(descriptor.getType()));
        String initializer = CodeInsightUtils.defaultInitializer(descriptor.getType());
        if (initializer != null) {
            bodyBuilder.append(" = ").append(initializer);
        }
        else {
            bodyBuilder.append(" = ?");
        }
        return JetPsiFactory.createProperty(project, bodyBuilder.toString());
    }

    private static String renderType(JetType type) {
        return DescriptorRenderer.TEXT.renderType(type);
    }

    private static JetElement overrideFunction(Project project, SimpleFunctionDescriptor descriptor) {
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append(displayableVisibility(descriptor));
        bodyBuilder.append("override fun ");

        List<String> whereRestrictions = new ArrayList<String>();
        if (!descriptor.getTypeParameters().isEmpty()) {
            bodyBuilder.append("<");
            boolean first = true;
            for (TypeParameterDescriptor param : descriptor.getTypeParameters()) {
                if (!first) {
                    bodyBuilder.append(", ");
                }

                bodyBuilder.append(param.getName());
                Set<JetType> upperBounds = param.getUpperBounds();
                if (!upperBounds.isEmpty()) {
                    boolean firstUpperBound = true;
                    for (JetType upperBound : upperBounds) {
                        String upperBoundText = " : " + renderType(upperBound);
                        if (!KotlinBuiltIns.getInstance().getDefaultBound().equals(upperBound)) {
                            if (firstUpperBound) {
                                bodyBuilder.append(upperBoundText);
                            }
                            else {
                                whereRestrictions.add(param.getName() + upperBoundText);
                            }
                        }
                        firstUpperBound = false;
                    }
                }

                first = false;
            }
            bodyBuilder.append("> ");
        }

        addReceiverParameter(descriptor, bodyBuilder);

        bodyBuilder.append(descriptor.getName()).append("(");
        boolean isAbstractFun = descriptor.getModality() == Modality.ABSTRACT;
        StringBuilder delegationBuilder = new StringBuilder();
        if (isAbstractFun) {
            delegationBuilder.append("throw UnsupportedOperationException()");
        }
        else {
            delegationBuilder.append("super<").append(descriptor.getContainingDeclaration().getName());
            delegationBuilder.append(">.").append(descriptor.getName()).append("(");
        }
        boolean first = true;
        for (ValueParameterDescriptor parameterDescriptor : descriptor.getValueParameters()) {
            if (!first) {
                bodyBuilder.append(",");
                if (!isAbstractFun) {
                    delegationBuilder.append(",");
                }
            }
            first = false;
            bodyBuilder.append(parameterDescriptor.getName());
            bodyBuilder.append(" : ");
            bodyBuilder.append(renderType(parameterDescriptor.getType()));

            if (!isAbstractFun) {
                delegationBuilder.append(parameterDescriptor.getName());
            }
        }
        bodyBuilder.append(")");
        if (!isAbstractFun) {
            delegationBuilder.append(")");
        }
        final JetType returnType = descriptor.getReturnType();
        final KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();

        boolean returnsNotUnit = returnType != null && !builtIns.getUnitType().equals(returnType);
        if (returnsNotUnit) {
            bodyBuilder.append(" : ").append(renderType(returnType));
        }
        if (!whereRestrictions.isEmpty()) {
            bodyBuilder.append("\n").append("where ").append(StringUtil.join(whereRestrictions, ", "));
        }
        bodyBuilder.append("{").append(returnsNotUnit && !isAbstractFun ? "return " : "").append(delegationBuilder.toString()).append("}");

        return JetPsiFactory.createFunction(project, bodyBuilder.toString());
    }

    private static void addReceiverParameter(CallableDescriptor descriptor, StringBuilder bodyBuilder) {
        ReceiverParameterDescriptor receiverParameter = descriptor.getReceiverParameter();
        if (receiverParameter != null) {
            bodyBuilder.append(receiverParameter.getType()).append(".");
        }
    }

    private static String displayableVisibility(MemberDescriptor descriptor) {
        Visibility visibility = descriptor.getVisibility().normalize();
        return visibility != Visibilities.INTERNAL ? visibility.toString() + " " : "";
    }

    @NotNull
    public Set<CallableMemberDescriptor> collectMethodsToGenerate(@NotNull JetClassOrObject classOrObject, BindingContext bindingContext) {
        final DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, classOrObject);
        if (descriptor instanceof MutableClassDescriptor) {
            return collectMethodsToGenerate((MutableClassDescriptor) descriptor);
        }
        return Collections.emptySet();
    }

    protected abstract Set<CallableMemberDescriptor> collectMethodsToGenerate(MutableClassDescriptor descriptor);

    private MemberChooser<DescriptorClassMember> showOverrideImplementChooser(
            Project project,
            DescriptorClassMember[] members
    ) {
        final MemberChooser<DescriptorClassMember> chooser = new MemberChooser<DescriptorClassMember>(members, true, true, project);
        chooser.setTitle(getChooserTitle());
        chooser.show();
        if (chooser.getExitCode() != DialogWrapper.OK_EXIT_CODE) return null;
        return chooser;
    }

    protected abstract String getChooserTitle();

    @Override
    public boolean isValidFor(Editor editor, PsiFile file) {
        if (!(file instanceof JetFile)) {
            return false;
        }
        final PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
        final JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, JetClassOrObject.class);
        return classOrObject != null;
    }

    protected abstract String getNoMethodsFoundHint();

    public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull PsiFile file, boolean implementAll) {
        final PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
        final JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, JetClassOrObject.class);

        assert classOrObject != null : "ClassObject should be checked in isValidFor method";

        BindingContext bindingContext =
                WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((JetFile) classOrObject.getContainingFile())
                        .getBindingContext();

        Set<CallableMemberDescriptor> missingImplementations = collectMethodsToGenerate(classOrObject, bindingContext);
        if (missingImplementations.isEmpty() && !implementAll) {
            HintManager.getInstance().showErrorHint(editor, getNoMethodsFoundHint());
            return;
        }
        List<DescriptorClassMember> members = membersFromDescriptors((JetFile) file, missingImplementations, bindingContext);

        final List<DescriptorClassMember> selectedElements;
        if (implementAll) {
            selectedElements = members;
        }
        else {
            final MemberChooser<DescriptorClassMember> chooser = showOverrideImplementChooser(
                    project,
                    members.toArray(new DescriptorClassMember[members.size()]));

            if (chooser == null) {
                return;
            }

            selectedElements = chooser.getSelectedElements();
            if (selectedElements == null || selectedElements.isEmpty()) return;
        }

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                generateMethods(editor, classOrObject, selectedElements);
            }
        });
    }

    @Override
    public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull PsiFile file) {
        invoke(project, editor, file, false);
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
