package org.jetbrains.jet.plugin.findUsages;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiConstructorCall;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;

public class FindUsagesUtils {
    private FindUsagesUtils() {
    }

    private static DeclarationDescriptor getCallDescriptor(PsiElement element, BindingContext bindingContext) {
        JetConstructorCalleeExpression constructorCalleeExpression =
                PsiTreeUtil.getParentOfType(element, JetConstructorCalleeExpression.class);
        if (constructorCalleeExpression != null) {
            JetReferenceExpression classReference = constructorCalleeExpression.getConstructorReferenceExpression();
            return bindingContext.get(BindingContext.REFERENCE_TARGET, classReference);
        }

        JetCallExpression callExpression = PsiTreeUtil.getParentOfType(element, JetCallExpression.class);
        if (callExpression != null) {
            JetExpression callee = callExpression.getCalleeExpression();
            if (callee instanceof JetReferenceExpression) {
                return bindingContext.get(BindingContext.REFERENCE_TARGET, (JetReferenceExpression) callee);
            }
        }

        return null;
    }

    public static boolean isConstructorUsage(PsiElement element, JetClassOrObject jetClassOrObject) {
        PsiConstructorCall constructorCall = PsiTreeUtil.getParentOfType(element, PsiConstructorCall.class);
        if (constructorCall != null && constructorCall == element.getParent()) {
            PsiMethod constructor = constructorCall.resolveConstructor();
            if (constructor == null) return false;

            PsiClass constructorClass = constructor.getContainingClass();
            return constructorClass != null && constructorClass.getNavigationElement() == jetClassOrObject;
        }
        if (!(element instanceof JetElement)) return false;

        BindingContext bindingContext =
                AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) element.getContainingFile()).getBindingContext();

       DeclarationDescriptor descriptor = getCallDescriptor(element, bindingContext);
       if (!(descriptor instanceof ConstructorDescriptor)) return false;

       DeclarationDescriptor containingDescriptor = descriptor.getContainingDeclaration();
       return containingDescriptor != null
              && BindingContextUtils.descriptorToDeclaration(bindingContext, containingDescriptor) == jetClassOrObject;
    }
}
