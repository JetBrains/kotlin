package org.jetbrains.jet.codegen;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.resolve.BindingContext;

/**
 * @author abreslav
 */
public class CodegenUtil {
    public static boolean isInterface(DeclarationDescriptor descriptor, BindingContext bindingContext) {
        PsiElement psiElement = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor);
        if(psiElement instanceof JetClass) {
            return ((JetClass)psiElement).isTrait();
        }
        if(psiElement instanceof PsiClass) {
            return ((PsiClass)psiElement).isInterface();
        }
        return false;
    }
}
