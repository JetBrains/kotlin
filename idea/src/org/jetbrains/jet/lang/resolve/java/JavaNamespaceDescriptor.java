package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.AbstractNamespaceDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.Annotation;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.JetScope;

import java.util.List;

/**
 * @author abreslav
 */
public class JavaNamespaceDescriptor extends AbstractNamespaceDescriptorImpl {
    private JetScope memberScope;

    public JavaNamespaceDescriptor(DeclarationDescriptor containingDeclaration, List<Annotation> annotations, String name) {
        super(containingDeclaration, annotations, name);
    }

    public void setMemberScope(@NotNull JetScope memberScope) {
        this.memberScope = memberScope;
    }

    @NotNull
    @Override
    public JetScope getMemberScope() {
        return memberScope;
    }
}
