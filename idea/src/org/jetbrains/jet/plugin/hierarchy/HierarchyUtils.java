package org.jetbrains.jet.plugin.hierarchy;

import com.google.common.base.Predicate;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.jet.lang.psi.*;

import javax.annotation.Nullable;

public class HierarchyUtils {
    public static final Predicate<PsiElement> IS_CALL_HIERARCHY_ELEMENT = new Predicate<PsiElement>() {
        @Override
        public boolean apply(@Nullable PsiElement input) {
            return input instanceof PsiMethod ||
                   input instanceof PsiClass ||
                   input instanceof JetFile ||
                   (input instanceof JetNamedFunction && !((JetNamedFunction) input).isLocal()) ||
                   input instanceof JetClassOrObject ||
                   (input instanceof JetProperty && !((JetProperty) input).isLocal());
        }
    };

    public static PsiElement getCallHierarchyElement(PsiElement element) {
        return JetPsiUtil.getParentByTypeAndPredicate(element, PsiElement.class, IS_CALL_HIERARCHY_ELEMENT, false);
    }
}
