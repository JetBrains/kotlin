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

package org.jetbrains.kotlin.idea.intentions.declarations;

import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.idea.JetBundle;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;

import java.util.List;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;
import static org.jetbrains.kotlin.resolve.BindingContext.DECLARATION_TO_DESCRIPTOR;

public class ConvertMemberToExtension extends BaseIntentionAction {

    public static final String CARET_ANCHOR = "____CARET_ANCHOR____";
    public static final String THROW_UNSUPPORTED_OPERATION_EXCEPTION = " throw UnsupportedOperationException()";

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
        JetCallableDeclaration declaration = getTarget(editor, file);
        if (declaration instanceof JetProperty) {
            if (((JetProperty) declaration).hasInitializer()) return false;
        }
        return declaration != null
               && !(declaration instanceof JetSecondaryConstructor)
               && declaration.getParent() instanceof JetClassBody
               && declaration.getParent().getParent() instanceof JetClass
               && declaration.getReceiverTypeReference() == null;
    }

    private static JetCallableDeclaration getTarget(Editor editor, PsiFile file) {
        PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
        return PsiTreeUtil.getParentOfType(element, JetCallableDeclaration.class, false, JetExpression.class);
    }

    @Override
    public void invoke(
            @NotNull Project project, Editor editor, PsiFile file
    ) throws IncorrectOperationException {
        JetCallableDeclaration member = getTarget(editor, file);
        assert member != null : "Must be checked by isAvailable";

        BindingContext bindingContext = ResolvePackage.analyzeFully(member);
        DeclarationDescriptor memberDescriptor = bindingContext.get(DECLARATION_TO_DESCRIPTOR, member);
        if (memberDescriptor == null) return;

        DeclarationDescriptor containingClass = memberDescriptor.getContainingDeclaration();
        assert containingClass instanceof ClassDescriptor : "Members must be contained in classes: \n" +
                                                            "Descriptor: " + memberDescriptor + "\n" +
                                                            "Declaration & context: " + member.getParent().getParent().getText();

        PsiElement outermostParent = JetPsiUtil.getOutermostParent(member, file, false);

        String receiver = ((ClassDescriptor) containingClass).getDefaultType() + ".";
        PsiElement identifier = member.getNameIdentifier();
        String name = identifier == null ? "" : identifier.getText();

        JetParameterList valueParameterList = member.getValueParameterList();
        JetTypeReference returnTypeRef = member.getTypeReference();

        String extensionText = modifiers(member) +
                               memberType(member) + " " +
                               typeParameters(member) +
                               receiver +
                               name +
                               (valueParameterList == null ? "" : valueParameterList.getText()) +
                               (returnTypeRef != null ? ": " + returnTypeRef.getText() : "") +
                               body(member);

        JetPsiFactory psiFactory = JetPsiFactory(member);
        JetDeclaration extension = psiFactory.<JetDeclaration>createDeclaration(extensionText);

        PsiElement added = file.addAfter(extension, outermostParent);
        file.addAfter(psiFactory.createNewLine(), outermostParent);
        file.addAfter(psiFactory.createNewLine(), outermostParent);
        member.delete();

        CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(added);

        int caretAnchor = added.getText().indexOf(CARET_ANCHOR);
        if (caretAnchor >= 0) {
            int caretOffset = added.getTextRange().getStartOffset() + caretAnchor;
            JetSimpleNameExpression anchor = PsiTreeUtil.findElementOfClassAtOffset(file, caretOffset, JetSimpleNameExpression.class, false);
            if (anchor != null && CARET_ANCHOR.equals(anchor.getReferencedName())) {
                JetExpression throwException = psiFactory.createExpression(THROW_UNSUPPORTED_OPERATION_EXCEPTION);
                PsiElement replaced = anchor.replace(throwException);
                TextRange range = replaced.getTextRange();
                editor.getCaretModel().moveToOffset(range.getStartOffset());
                editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset());
            }
        }
    }

    private static String memberType(JetCallableDeclaration member) {
        if (member instanceof JetFunction) {
            return "fun";
        }
        return ((JetProperty) member).getValOrVarNode().getText();
    }

    private static String modifiers(JetCallableDeclaration member) {
        JetModifierList modifierList = member.getModifierList();
        if (modifierList == null) return "";
        for (IElementType modifierType : JetTokens.VISIBILITY_MODIFIERS.getTypes()) {
            PsiElement modifier = modifierList.getModifier((JetModifierKeywordToken) modifierType);
            if (modifier != null) {
                return modifierType == JetTokens.PROTECTED_KEYWORD ? "" : modifier.getText() + " ";
            }
        }
        return "";
    }

    private static String typeParameters(JetCallableDeclaration member) {
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

    private static String body(JetCallableDeclaration member) {
        if (member instanceof JetProperty) {
            JetProperty property = (JetProperty) member;
            return "\n" + getter(property) + "\n" + setter(property, !synthesizeBody(property.getGetter()));
        }
        else if (member instanceof JetFunction) {
            JetFunction function = (JetFunction) member;
            JetExpression bodyExpression = function.getBodyExpression();
            if (bodyExpression == null) return "{" + CARET_ANCHOR + "}";
            if (!function.hasBlockBody()) return " = " + bodyExpression.getText();
            return bodyExpression.getText();
        }
        else {
            return "";
        }
    }

    private static String getter(JetProperty property) {
        JetPropertyAccessor getter = property.getGetter();
        if (synthesizeBody(getter)) return "get() = " + CARET_ANCHOR;
        return getter.getText();
    }

    private static String setter(JetProperty property, boolean allowCaretAnchor) {
        if (!property.isVar()) return "";
        JetPropertyAccessor setter = property.getSetter();
        if (synthesizeBody(setter)) return "set(value) {" + (allowCaretAnchor ? CARET_ANCHOR : THROW_UNSUPPORTED_OPERATION_EXCEPTION) + "}";
        return setter.getText();
    }

    private static boolean synthesizeBody(@Nullable JetPropertyAccessor getter) {
        return getter == null || getter.getBodyExpression() == null;
    }
}
