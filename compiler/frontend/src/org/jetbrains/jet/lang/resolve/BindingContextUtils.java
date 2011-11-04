package org.jetbrains.jet.lang.resolve;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.psi.*;

/**
 * @author abreslav
 */
public class BindingContextUtils {
    private BindingContextUtils() {
    }

    public static PsiElement resolveToDeclarationPsiElement(BindingContext bindingContext, JetReferenceExpression referenceExpression) {
        DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, referenceExpression);
        if (declarationDescriptor == null) {
            return bindingContext.get(BindingContext.LABEL_TARGET, referenceExpression);
        }
        return bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, declarationDescriptor);
    }

    @Nullable
    public static VariableDescriptor extractVariableDescriptorIfAny(BindingContext bindingContext, JetElement element, boolean onlyReference) {
        DeclarationDescriptor descriptor = null;
        if (!onlyReference && (element instanceof JetProperty || element instanceof JetParameter)) {
            descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
        }
        else if (element instanceof JetSimpleNameExpression) {
            descriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, (JetSimpleNameExpression) element);
        }
        else if (element instanceof JetQualifiedExpression) {
            descriptor = extractVariableDescriptorIfAny(bindingContext, ((JetQualifiedExpression) element).getSelectorExpression(), onlyReference);
        }
        if (descriptor instanceof VariableDescriptor) {
            return (VariableDescriptor) descriptor;
        }
        return null;
    }
}
