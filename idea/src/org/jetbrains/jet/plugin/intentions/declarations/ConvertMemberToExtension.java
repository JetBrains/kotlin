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

package org.jetbrains.jet.plugin.intentions.declarations;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManagerUtil;

import java.util.List;

public class ConvertMemberToExtension extends BaseIntentionAction {

    @NotNull
    @Override
    public String getText() {
        return getFamilyName();
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("convert.to.extension");
    }

    @Override
    public boolean isAvailable(
            @NotNull Project project, Editor editor, PsiFile file
    ) {
        JetNamedFunction function = getTarget(editor, file);
        return function != null
               && function.getParent() instanceof JetClassBody
               && function.getParent().getParent() instanceof JetClass
               && function.getReceiverTypeRef() == null;
    }

    private static JetNamedFunction getTarget(Editor editor, PsiFile file) {
        PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
        return PsiTreeUtil.getParentOfType(element, JetNamedFunction.class, false, JetExpression.class);
    }

    @Override
    public void invoke(
            @NotNull Project project, Editor editor, PsiFile file
    ) throws IncorrectOperationException {
        JetNamedFunction member = getTarget(editor, file);
        assert member != null : "Must be checked by isAvailable";

        BindingContext bindingContext = KotlinCacheManagerUtil.getDeclarationsBindingContext(member);
        SimpleFunctionDescriptor memberDescriptor = bindingContext.get(BindingContext.FUNCTION, member);

        if (memberDescriptor == null) return;
        assert memberDescriptor.getReceiverParameter() == null : "Must have checked that there's no receiver earlier\n" +
                                                                 "Descriptor: " + memberDescriptor + "\n" +
                                                                 "Declaration: " + member.getText();

        DeclarationDescriptor containingClass = memberDescriptor.getContainingDeclaration();
        assert containingClass instanceof ClassDescriptor : "Members must be contained in classes: \n" +
                                                            "Descriptor: " + memberDescriptor + "\n" +
                                                            "Declaration & context: " + member.getParent().getParent().getText();

        PsiElement outermostParent = JetPsiUtil.getOutermostParent(member, file, false);

        String receiver = ((ClassDescriptor) containingClass).getDefaultType() + ".";
        PsiElement identifier = member.getNameIdentifier();
        String name = identifier == null ? "" : identifier.getText();

        JetParameterList valueParameterList = member.getValueParameterList();
        JetTypeReference returnTypeRef = member.getReturnTypeRef();
        String receiverAndTheRest = receiver +
                                   name +
                                   (valueParameterList == null ? "" : valueParameterList.getText()) +
                                   (returnTypeRef != null ? ": " + returnTypeRef.getText() : "") +
                                   textOfBody(member);

        String extensionText = modifiers(member) + "fun " + typeParameters(member) + receiverAndTheRest;

        JetNamedFunction extension = JetPsiFactory.createFunction(project, extensionText);

        file.addAfter(extension, outermostParent);
        file.addAfter(JetPsiFactory.createNewLine(project), outermostParent);
        file.addAfter(JetPsiFactory.createNewLine(project), outermostParent);
        member.delete();
    }

    private static String modifiers(JetNamedFunction member) {
        JetModifierList modifierList = member.getModifierList();
        if (modifierList == null) return "";
        for (IElementType modifierType : JetTokens.VISIBILITY_MODIFIERS.getTypes()) {
            PsiElement modifier = modifierList.getModifier((JetToken) modifierType);
            if (modifier != null) {
                return modifierType == JetTokens.PROTECTED_KEYWORD ? "" : modifier.getText() + " ";
            }
        }
        return "";
    }

    private static String typeParameters(JetNamedFunction member) {
        PsiElement classElement = member.getParent().getParent();
        assert classElement instanceof JetClass : "Must be checked in isAvailable: " + classElement.getText();

        List<JetTypeParameter> allTypeParameters = ContainerUtil.concat(
                ((JetClass) classElement).getTypeParameters(),
                member.getTypeParameters());
        if (allTypeParameters.isEmpty()) return "";
        return "<" + StringUtil.join(allTypeParameters, new Function<JetTypeParameter, String>() {
            @Override
            public String fun(JetTypeParameter parameter) {
                return parameter.getText();
            }
        }, ", ") + "> ";
    }

    private static String textOfBody(JetNamedFunction member) {
        JetExpression bodyExpression = member.getBodyExpression();
        if (bodyExpression == null) return "{}";
        if (!member.hasBlockBody()) return " = " + bodyExpression.getText();
        return bodyExpression.getText();
    }

}
