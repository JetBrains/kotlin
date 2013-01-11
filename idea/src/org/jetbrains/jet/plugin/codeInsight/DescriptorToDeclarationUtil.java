package org.jetbrains.jet.plugin.codeInsight;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.plugin.references.BuiltInsReferenceResolver;

import java.util.Collection;

public final class DescriptorToDeclarationUtil {
    private DescriptorToDeclarationUtil() {
    }

    public static PsiElement getDeclaration(JetFile file, DeclarationDescriptor descriptor, BindingContext bindingContext) {
        Collection<PsiElement> elements = BindingContextUtils.descriptorToDeclarations(bindingContext, descriptor);

        if (elements.isEmpty()) {
            BuiltInsReferenceResolver libraryReferenceResolver =
                    file.getProject().getComponent(BuiltInsReferenceResolver.class);
            elements = libraryReferenceResolver.resolveStandardLibrarySymbol(descriptor);
        }

        if (!elements.isEmpty()) {
            return elements.iterator().next();
        }

        return null;
    }
}
