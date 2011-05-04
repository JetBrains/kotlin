package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/**
 * @author abreslav
 */
public class ModuleDescriptor extends DeclarationDescriptorImpl {
    public ModuleDescriptor(String name) {
        super(null, Collections.<Annotation>emptyList(), name);
    }

    @NotNull
    @Override
    public ModuleDescriptor substitute(TypeSubstitutor substitutor) {
        return this;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitModuleDeclaration(this, data);
    }
}
