/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.plugin.compiler.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.plugin.quickfix.ImportInsertHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author yole
 */
public abstract class OverrideImplementMethodsHandler implements LanguageCodeInsightActionHandler {
    public static List<DescriptorClassMember> membersFromDescriptors(Iterable<CallableMemberDescriptor> missingImplementations) {
        List<DescriptorClassMember> members = new ArrayList<DescriptorClassMember>();
        for (CallableMemberDescriptor memberDescriptor : missingImplementations) {
            members.add(new DescriptorClassMember(memberDescriptor));
        }
        return members;
    }

    public Set<CallableMemberDescriptor> collectMethodsToGenerate(JetClassOrObject classOrObject) {
        BindingContext bindingContext =
            WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((JetFile)classOrObject.getContainingFile());
        final DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, classOrObject);
        if (descriptor instanceof MutableClassDescriptor) {
            return collectMethodsToGenerate((MutableClassDescriptor)descriptor);
        }
        return Collections.emptySet();
    }

    protected abstract Set<CallableMemberDescriptor> collectMethodsToGenerate(MutableClassDescriptor descriptor);

    public static void generateMethods(Project project,
                                       Editor editor,
                                       JetClassOrObject classOrObject,
                                       List<DescriptorClassMember> selectedElements) {
        final JetClassBody body = classOrObject.getBody();
        if (body == null) {
            return;
        }

        // NOTE + TODO: If you try to cache findInsertBeforeAnchor element, there will be failed assertion
        // "PSI/document inconsistency before reparse: file=" from DocumentCommitThread after inserting two overriding
        // with the caret right before existing function start.

        PsiElement afterAnchor = findInsertAfterAnchor(editor, body);

        if (afterAnchor == null) {
            return;
        }

        for (DescriptorClassMember selectedElement : selectedElements) {
            final DeclarationDescriptor descriptor = selectedElement.getDescriptor();
            JetFile containingFile = (JetFile)classOrObject.getContainingFile();
            if (descriptor instanceof SimpleFunctionDescriptor) {
                JetElement target = overrideFunction(project, containingFile, (SimpleFunctionDescriptor)descriptor);
                afterAnchor = body.addAfter(target, afterAnchor);
            }
            else if (descriptor instanceof PropertyDescriptor) {
                JetElement target = overrideProperty(project, containingFile, (PropertyDescriptor)descriptor);
                afterAnchor = body.addBefore(target, afterAnchor);
            }
        }
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

        if (offsetCursorElement != null) {
            afterAnchor = offsetCursorElement;
        }

        return afterAnchor;
    }

    private static JetElement overrideProperty(Project project, JetFile file, PropertyDescriptor descriptor) {
        StringBuilder bodyBuilder = new StringBuilder("override ");
        if (descriptor.isVar()) {
            bodyBuilder.append("var ");
        }
        else {
            bodyBuilder.append("val ");
        }
        bodyBuilder.append(descriptor.getName()).append(":").append(descriptor.getType());
        ImportInsertHelper.addImportDirectiveIfNeeded(descriptor.getType(), file);
        String initializer = defaultInitializer(descriptor.getType(), JetStandardLibrary.getInstance());
        if (initializer != null) {
            bodyBuilder.append("=").append(initializer);
        }
        else {
            bodyBuilder.append("= ?");
        }
        return JetPsiFactory.createProperty(project, bodyBuilder.toString());
    }

    private static JetElement overrideFunction(Project project, JetFile file, SimpleFunctionDescriptor descriptor) {
        StringBuilder bodyBuilder = new StringBuilder("override fun ");
        bodyBuilder.append(descriptor.getName());
        bodyBuilder.append("(");
        boolean first = true;
        for (ValueParameterDescriptor parameterDescriptor : descriptor.getValueParameters()) {
            if (!first) {
                bodyBuilder.append(",");
            }
            first = false;
            bodyBuilder.append(parameterDescriptor.getName());
            bodyBuilder.append(":");
            bodyBuilder.append(parameterDescriptor.getType().toString());

            ImportInsertHelper.addImportDirectiveIfNeeded(parameterDescriptor.getType(), file);
        }
        bodyBuilder.append(")");
        final JetType returnType = descriptor.getReturnType();
        final JetStandardLibrary stdlib = JetStandardLibrary.getInstance();
        if (!returnType.equals(stdlib.getTuple0Type())) {
            bodyBuilder.append(":").append(returnType.toString());
            ImportInsertHelper.addImportDirectiveIfNeeded(returnType, file);
        }

        bodyBuilder.append("{").append("throw UnsupportedOperationException()").append("}");

        return JetPsiFactory.createFunction(project, bodyBuilder.toString());
    }

    private static String defaultInitializer(JetType returnType, JetStandardLibrary stdlib) {
        if (returnType.isNullable()) {
            return "null";
        }
        else if (returnType.equals(stdlib.getIntType()) || returnType.equals(stdlib.getLongType()) ||
                 returnType.equals(stdlib.getShortType()) || returnType.equals(stdlib.getByteType()) ||
                 returnType.equals(stdlib.getFloatType()) || returnType.equals(stdlib.getDoubleType())) {
            return "0";
        }
        else if (returnType.equals(stdlib.getBooleanType())) {
            return "false";
        }
        return null;
    }

    private MemberChooser<DescriptorClassMember> showOverrideImplementChooser(Project project,
                                                                              DescriptorClassMember[] members) {
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

    public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull PsiFile file,
                       boolean implementAll) {
        final PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
        final JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, JetClassOrObject.class);
        Set<CallableMemberDescriptor> missingImplementations = collectMethodsToGenerate(classOrObject);
        if (missingImplementations.isEmpty() && !implementAll) {
            HintManager.getInstance().showErrorHint(editor, getNoMethodsFoundHint());
            return;
        }
        List<DescriptorClassMember> members = membersFromDescriptors(missingImplementations);

        final List<DescriptorClassMember> selectedElements;
        if (implementAll) {
            selectedElements = members;
        }
        else {
            final MemberChooser<DescriptorClassMember> chooser = showOverrideImplementChooser(project,
                                                                                              members.toArray(
                                                                                                  new DescriptorClassMember[members
                                                                                                      .size()]));
            if (chooser == null) {
                return;
            }

            selectedElements = chooser.getSelectedElements();
            if (selectedElements == null || selectedElements.isEmpty()) return;
        }

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                generateMethods(project, editor, classOrObject, selectedElements);
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
