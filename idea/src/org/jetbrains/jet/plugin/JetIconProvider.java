package org.jetbrains.jet.plugin;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetTokens;

import javax.swing.*;

/**
 * @author yole
 */
public class JetIconProvider extends IconProvider {
    public static final Icon ICON_FOR_OBJECT = PlatformIcons.ANONYMOUS_CLASS_ICON;

    @Override
    public Icon getIcon(@NotNull PsiElement psiElement, int flags) {
        if (psiElement instanceof JetNamespaceHeader) {
            return (flags & Iconable.ICON_FLAG_OPEN) != 0 ? PlatformIcons.PACKAGE_OPEN_ICON : PlatformIcons.PACKAGE_ICON;
        }
        if (psiElement instanceof JetNamedFunction) {
            return PsiTreeUtil.getParentOfType(psiElement, JetNamedDeclaration.class) instanceof JetClass
                   ? PlatformIcons.METHOD_ICON
                   : PlatformIcons.FUNCTION_ICON;
        }
        if (psiElement instanceof JetClass) {
            JetClass jetClass = (JetClass) psiElement;
            Icon icon = jetClass.hasModifier(JetTokens.ENUM_KEYWORD) ? PlatformIcons.ENUM_ICON : PlatformIcons.CLASS_ICON;
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
                    return PlatformIcons.PROPERTY_ICON;
                }
            }
            return PlatformIcons.PARAMETER_ICON;
        }
        if (psiElement instanceof JetProperty) {
            return PlatformIcons.PROPERTY_ICON;
        }
        return null;
    }
}
