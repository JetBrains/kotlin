package org.jetbrains.jet.lang.types;

import org.jetbrains.jet.lang.psi.JetElement;

import java.util.List;

/**
 * @author abreslav
 */
public abstract class DeclarationDescriptorImpl<T extends JetElement> extends AnnotatedImpl implements Named, DeclarationDescriptor {

    private final String name;
    private final T psiElement;

    public DeclarationDescriptorImpl(T psiElement, List<Attribute> attributes, String name) {
        super(attributes);
        this.name = name;
        this.psiElement = psiElement;
    }

    @Override
    public String getName() {
        return name;
    }

    public T getPsiElement() {
        return psiElement;
    }
}
