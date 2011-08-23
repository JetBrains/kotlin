package org.jetbrains.jet.lang.resolve;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;

/**
 * @author abreslav
 */
public class BindingContextUtils {
    public static PsiElement resolveToDeclarationPsiElement(BindingContext bindingContext, JetReferenceExpression referenceExpression) {
        DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, referenceExpression);
        if (declarationDescriptor == null) {
            return bindingContext.get(BindingContext.LABEL_TARGET, referenceExpression);
        }
        return bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, declarationDescriptor);
    }

}
