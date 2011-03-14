package org.jetbrains.jet.lang.types;

import java.util.Collections;

/**
 * @author abreslav
 */
public class ModuleDescriptor extends DeclarationDescriptorImpl {
    public ModuleDescriptor(String name) {
        super(null, Collections.<Attribute>emptyList(), name);
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitModuleDeclaration(this, data);
    }
}
