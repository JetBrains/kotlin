package org.jetbrains.jet.plugin;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Icons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetTokens;

import javax.swing.*;

/**
 * @author yole
 */
public class JetIconProvider extends IconProvider {
    public static final Icon ICON_FOR_OBJECT = Icons.ANONYMOUS_CLASS_ICON;

    @Override
    public Icon getIcon(@NotNull PsiElement psiElement, int flags) {
        if (psiElement instanceof JetNamespace) {
            return (flags & Iconable.ICON_FLAG_OPEN) != 0 ? Icons.PACKAGE_OPEN_ICON : Icons.PACKAGE_ICON;
        }
        if (psiElement instanceof JetFunction) {
            return PsiTreeUtil.getParentOfType(psiElement, JetNamedDeclaration.class) instanceof JetClass
                    ? Icons.METHOD_ICON
                    : Icons.FUNCTION_ICON;
        }
        if (psiElement instanceof JetClass) {
            JetClass jetClass = (JetClass) psiElement;
            Icon icon = jetClass.hasModifier(JetTokens.ENUM_KEYWORD) ? Icons.ENUM_ICON : Icons.CLASS_ICON;
            if (jetClass instanceof JetEnumEntry) {
                JetEnumEntry enumEntry = (JetEnumEntry) jetClass;
                if (enumEntry.getPrimaryConstructorParameterList() == null) {
                    icon = ICON_FOR_OBJECT;
                }
            }
            return icon;
        }
        if (psiElement instanceof JetParameter) {
            if (((JetParameter) psiElement).getValOrVarNode() != null) {
                JetParameterList parameterList = PsiTreeUtil.getParentOfType(psiElement, JetParameterList.class);
                if (parameterList != null && parameterList.getParent() instanceof JetClass) {
                    return Icons.PROPERTY_ICON;
                }
            }
            return Icons.PARAMETER_ICON;
        }
        if (psiElement instanceof JetProperty) {
            return Icons.PROPERTY_ICON;
        }
        return null;
    }
}
