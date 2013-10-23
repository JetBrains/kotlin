package org.jetbrains.jet.plugin.hierarchy;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import jet.Function1;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.psi.psiUtil.PsiUtilPackage;

import javax.annotation.Nullable;

public class HierarchyUtils {
    public static final Function1<PsiElement, Boolean> IS_CALL_HIERARCHY_ELEMENT = new Function1<PsiElement, Boolean>() {
        @Override
        public Boolean invoke(@Nullable PsiElement input) {
            return input instanceof PsiMethod ||
                   input instanceof PsiClass ||
                   input instanceof JetFile ||
                   input instanceof JetNamedFunction ||
                   input instanceof JetClassOrObject ||
                   input instanceof JetProperty;
        }
    };

    public static PsiElement getCallHierarchyElement(PsiElement element) {
        return PsiUtilPackage.getParentByTypeAndPredicate(element, PsiElement.class, false, IS_CALL_HIERARCHY_ELEMENT);
    }
}
