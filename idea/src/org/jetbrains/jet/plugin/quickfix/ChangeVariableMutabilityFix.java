package org.jetbrains.jet.plugin.quickfix;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.diagnostics.DiagnosticParameter;
import org.jetbrains.jet.lang.diagnostics.DiagnosticParameters;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithPsiElement;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacade;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;

import java.util.Arrays;

/**
 * @author svtk
 */
public class ChangeVariableMutabilityFix implements IntentionAction {
    private boolean isVar;
    
    public ChangeVariableMutabilityFix(boolean isVar) {
        this.isVar = isVar;
    }

    public ChangeVariableMutabilityFix() {
        this(false);
    }
    
    @NotNull
    @Override
    public String getText() {
        return isVar ? JetBundle.message("make.variable.immutable") : JetBundle.message("make.variable.mutable");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("change.variable.mutability.family");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!(file instanceof JetFile)) return false;
        JetProperty property = getCorrespondingProperty(editor, (JetFile) file);
        return property != null && !property.isVar();
    }

    private static JetProperty getCorrespondingProperty(Editor editor, JetFile file) {
        final PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
        JetProperty property = PsiTreeUtil.getParentOfType(elementAtCaret, JetProperty.class);
        if (property != null) return property;
        JetSimpleNameExpression simpleNameExpression = PsiTreeUtil.getParentOfType(elementAtCaret, JetSimpleNameExpression.class);
        if (simpleNameExpression != null) {
            BindingContext bindingContext = AnalyzerFacade.analyzeFileWithCache(file, AnalyzerFacade.SINGLE_DECLARATION_PROVIDER);
            VariableDescriptor descriptor = BindingContextUtils.extractVariableDescriptorIfAny(bindingContext, simpleNameExpression, true);
            if (descriptor != null) {
                PsiElement declaration = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor);
                if (declaration instanceof JetProperty) {
                    return (JetProperty) declaration;
                }
            }
        }
        return null;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        JetProperty element = getCorrespondingProperty(editor, (JetFile)file);
        JetProperty newElement = (JetProperty) element.copy();
        if (newElement.isVar()) {
            PsiElement varElement = newElement.getNode().findChildByType(JetTokens.VAR_KEYWORD).getPsi();

            JetProperty valProperty = JetPsiFactory.createProperty(project, "x", "Any", false);
            PsiElement valElement = valProperty.getNode().findChildByType(JetTokens.VAL_KEYWORD).getPsi();
            CodeEditUtil.replaceChild(newElement.getNode(), varElement.getNode(), valElement.getNode());
        }
        else {
            PsiElement valElement = newElement.getNode().findChildByType(JetTokens.VAL_KEYWORD).getPsi();

            JetProperty varProperty = JetPsiFactory.createProperty(project, "x", "Any", true);
            PsiElement varElement = varProperty.getNode().findChildByType(JetTokens.VAR_KEYWORD).getPsi();
            CodeEditUtil.replaceChild(newElement.getNode(), valElement.getNode(), varElement.getNode());
        }
        element.replace(newElement);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
