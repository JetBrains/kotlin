package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.AbstractNamespaceDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.List;

/**
 * @author abreslav
 */
public class JavaNamespaceDescriptor extends AbstractNamespaceDescriptorImpl {
    private JetScope memberScope;
    private final String qualifiedName;
    /** Namespace of class with static methods */
    private final boolean namespace;
    
    public JavaNamespaceDescriptor(DeclarationDescriptor containingDeclaration, List<AnnotationDescriptor> annotations,
            @NotNull String name, @NotNull String qualifiedName, boolean namespace) {
        super(containingDeclaration, annotations, name);
        this.qualifiedName = qualifiedName;
        this.namespace = namespace;
    }

    public void setMemberScope(@NotNull JetScope memberScope) {
        this.memberScope = memberScope;
    }

    @NotNull
    @Override
    public JetScope getMemberScope() {
        return memberScope;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public boolean isNamespace() {
        return namespace;
    }
}
