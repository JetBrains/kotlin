package org.jetbrains.jet.plugin.refactoring.safeDelete;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.safeDelete.JavaSafeDeleteDelegate;
import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceSimpleDeleteUsageInfo;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.jet.asJava.AsJavaPackage;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.plugin.references.JetPsiReference;

import java.util.List;

public class KotlinJavaSafeDeleteDelegate implements JavaSafeDeleteDelegate {
    @Override
    public void createUsageInfoForParameter(PsiReference reference, List<UsageInfo> usages, PsiParameter parameter, PsiMethod method) {
        if (reference instanceof JetPsiReference) {
            JetElement element = (JetElement) reference.getElement();

            PsiElement originalDeclaration = AsJavaPackage.getUnwrapped(method);
            if (!(originalDeclaration instanceof PsiMethod || originalDeclaration instanceof JetDeclaration)) return;

            int parameterIndex = method.getParameterList().getParameterIndex(parameter);
            if (PsiUtilPackage.isExtensionDeclaration(originalDeclaration)) {
                parameterIndex--;
            }
            if (parameterIndex < 0) return;

            JetCallExpression callExpression = PsiTreeUtil.getParentOfType(reference.getElement(), JetCallExpression.class, false);
            if (callExpression == null) return;

            JetExpression calleeExpression = callExpression.getCalleeExpression();
            if (!(calleeExpression instanceof JetReferenceExpression && PsiTreeUtil.isAncestor(calleeExpression, element, false))) return;

            BindingContext bindingContext = AnalyzerFacadeWithCache.getContextForElement(element);

            DeclarationDescriptor descriptor =
                    bindingContext.get(BindingContext.REFERENCE_TARGET, (JetReferenceExpression) calleeExpression);
            if (descriptor == null) return;

            PsiElement declaration = BindingContextUtils.descriptorToDeclaration(bindingContext, descriptor);
            if (originalDeclaration.equals(declaration)) {
                List<? extends ValueArgument> args = callExpression.getValueArguments();
                int argCount = args.size();
                if (parameterIndex < argCount) {
                    usages.add(
                            new SafeDeleteValueArgumentListUsageInfo((JetValueArgument) args.get(parameterIndex), parameter)
                    );
                }
                else {
                    List<JetExpression> lambdaArgs = callExpression.getFunctionLiteralArguments();
                    int lambdaIndex = parameterIndex - argCount;
                    if (lambdaIndex < lambdaArgs.size()) {
                        usages.add(new SafeDeleteReferenceSimpleDeleteUsageInfo(lambdaArgs.get(lambdaIndex), parameter, true));
                    }
                }
            }
        }
    }
}
