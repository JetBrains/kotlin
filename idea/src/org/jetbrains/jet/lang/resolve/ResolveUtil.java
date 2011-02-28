package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.psi.JetTypeParameter;
import org.jetbrains.jet.lang.types.*;

/**
 * @author abreslav
 */
public class ResolveUtil {
    @Nullable
    private static <T extends JetElement> T getDeclarationElement(Object o) {
        if (o instanceof DeclarationDescriptorImpl) {
            @SuppressWarnings({"unchecked", "UnnecessaryLocalVariable"})
            T psiElement = ((DeclarationDescriptorImpl<T>) o).getPsiElement();
            return psiElement;
        }
        return null;
    }

    @Nullable
    private static <T extends JetElement> T getDeclarationElement(DeclarationDescriptorImpl<T> descriptor) {
        return descriptor.getPsiElement();
    }

    @Nullable
    public static JetTypeParameter getJetTypeParameter(TypeParameterDescriptor parameterDescriptor) {
        return getDeclarationElement(parameterDescriptor);
    }

    @Nullable
    public static JetParameter getJetParameter(ValueParameterDescriptor parameterDescriptor) {
        return (JetParameter) getDeclarationElement(parameterDescriptor);
    }

    @Nullable
    public static JetFunction getJetFunction(FunctionDescriptor functionDescriptor) {
        return getDeclarationElement(functionDescriptor);
    }
}
