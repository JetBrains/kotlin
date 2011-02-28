package org.jetbrains.jet.lang.types;

import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetProperty;

import java.util.List;

/**
 * @author abreslav
 */
public class PropertyDescriptorImpl extends DeclarationDescriptorImpl<JetDeclaration> implements PropertyDescriptor {
    private Type type;

    public PropertyDescriptorImpl(JetDeclaration psiElement, List<Attribute> attributes, String name, Type type) {
        super(psiElement, attributes, name);
        this.type = type;
    }

    @Override
    public Type getType() {
        return type;
    }
}
