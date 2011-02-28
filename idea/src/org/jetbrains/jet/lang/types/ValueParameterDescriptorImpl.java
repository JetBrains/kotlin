package org.jetbrains.jet.lang.types;

import com.intellij.psi.PsiElement;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.psi.JetParameterList;

import java.util.List;

/**
 * @author abreslav
 */
public class ValueParameterDescriptorImpl extends PropertyDescriptorImpl implements ValueParameterDescriptor {
    private final boolean hasDefaultValue;
    private final boolean isVararg;

    public ValueParameterDescriptorImpl(JetParameter psiElement, List<Attribute> attributes, String name, Type type, boolean hasDefaultValue, boolean isVararg) {
        super(psiElement, attributes, name, type);
        this.hasDefaultValue = hasDefaultValue;
        this.isVararg = isVararg;
    }

    @Override
    public int getIndex() {
        final JetDeclaration element = getPsiElement();
        final PsiElement parent = element.getParent();
        if (parent instanceof JetParameterList) {
            return ((JetParameterList) parent).getParameters().indexOf(element);
        }
        throw new IllegalStateException("couldn't find index for parameter");
    }

    @Override
    public boolean hasDefaultValue() {
        return hasDefaultValue;
    }

    @Override
    public boolean isRef() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public boolean isVararg() {
        return isVararg;
    }
}
