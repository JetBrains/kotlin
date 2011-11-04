package org.jetbrains.jet.plugin.codeInsight;

import com.google.common.collect.Sets;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.ide.util.MemberChooser;
import com.intellij.lang.LanguageCodeInsightActionHandler;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.OverrideResolver;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.compiler.WholeProjectAnalyzerFacade;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author yole
 */
public class ImplementMethodsHandler implements LanguageCodeInsightActionHandler {
    @Override
    public boolean isValidFor(Editor editor, PsiFile file) {
        if (!(file instanceof JetFile)) {
            return false;
        }
        final PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
        final JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, JetClassOrObject.class);
        return classOrObject != null;
    }

    @Override
    public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull PsiFile file) {
        final PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
        final JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, JetClassOrObject.class);
        Set<CallableMemberDescriptor> missingImplementations = collectMethodsToImplement(classOrObject);
        if (missingImplementations.isEmpty()) {
            HintManager.getInstance().showErrorHint(editor, "No methods to implement have been found");
            return;
        }
        List<DescriptorClassMember> members = membersFromDescriptors(missingImplementations);
        final MemberChooser<DescriptorClassMember> chooser = showOverrideImplementChooser(project,
                                                                                          members.toArray(new DescriptorClassMember[members.size()]));
        if (chooser == null) {
            return;
        }

        final List<DescriptorClassMember> selectedElements = chooser.getSelectedElements();
        if (selectedElements == null || selectedElements.isEmpty()) return;

        new WriteCommandAction(project, file) {
          protected void run(final Result result) throws Throwable {
            overrideOrImplementMethodsInRightPlace(project, editor, classOrObject, selectedElements);
          }
        }.execute();

    }

    public static List<DescriptorClassMember> membersFromDescriptors(Set<CallableMemberDescriptor> missingImplementations) {
        List<DescriptorClassMember> members = new ArrayList<DescriptorClassMember>();
        for (CallableMemberDescriptor memberDescriptor : missingImplementations) {
            members.add(new DescriptorClassMember(memberDescriptor));
        }
        return members;
    }

    public static Set<CallableMemberDescriptor> collectMethodsToImplement(JetClassOrObject classOrObject) {
        BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile((JetFile) classOrObject.getContainingFile());
        final DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, classOrObject);
        Set<CallableMemberDescriptor> missingImplementations = Sets.newLinkedHashSet();
        if (descriptor instanceof MutableClassDescriptor) {
            OverrideResolver.collectMissingImplementations((MutableClassDescriptor) descriptor,
                                                           missingImplementations, missingImplementations);
        }
        return missingImplementations;
    }

    public static void overrideOrImplementMethodsInRightPlace(Project project, Editor editor, JetClassOrObject classOrObject, List<DescriptorClassMember> selectedElements) {
        final JetClassBody body = classOrObject.getBody();
        if (body == null) {
            return;
        }
        for (DescriptorClassMember selectedElement : selectedElements) {
            final DeclarationDescriptor descriptor = selectedElement.getDescriptor();
            if (descriptor instanceof FunctionDescriptor) {
                JetElement target = overrideFunction(project, (FunctionDescriptor) descriptor);
                body.addBefore(target, body.getRBrace());
            }
        }
    }

    private static JetElement overrideFunction(Project project, FunctionDescriptor descriptor) {
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
            bodyBuilder.append(": ");
            bodyBuilder.append(parameterDescriptor.getOutType().toString());
        }
        bodyBuilder.append(")");
        final JetType returnType = descriptor.getReturnType();
        final JetStandardLibrary stdlib = JetStandardLibrary.getJetStandardLibrary(project);
        if (!returnType.equals(stdlib.getTuple0Type())) {
            bodyBuilder.append(": ").append(returnType.toString());
        }
        bodyBuilder.append("{");
        if (returnType.isNullable()) {
            bodyBuilder.append("return null");
        }
        else if (returnType.equals(stdlib.getIntType()) || returnType.equals(stdlib.getLongType()) ||
                 returnType.equals(stdlib.getShortType()) || returnType.equals(stdlib.getByteType()) ||
                 returnType.equals(stdlib.getFloatType()) || returnType.equals(stdlib.getDoubleType())) {
            bodyBuilder.append("return 0");
        }
        else if (returnType.equals(stdlib.getBooleanType())) {
            bodyBuilder.append("return false");
        }
        bodyBuilder.append("}");
        return JetPsiFactory.createFunction(project, bodyBuilder.toString());
    }

    private static MemberChooser<DescriptorClassMember> showOverrideImplementChooser(Project project,
                                                                                     DescriptorClassMember[] members) {
        final MemberChooser<DescriptorClassMember> chooser = new MemberChooser<DescriptorClassMember>(members, true, true, project);
        chooser.setTitle("Implement Members");
        chooser.show();
        if (chooser.getExitCode() != DialogWrapper.OK_EXIT_CODE) return null;
        return chooser;
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }
}
