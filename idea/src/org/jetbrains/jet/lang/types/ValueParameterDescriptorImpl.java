package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author abreslav
 */
public class ValueParameterDescriptorImpl extends PropertyDescriptorImpl implements ValueParameterDescriptor {
    private final boolean hasDefaultValue;
    private final boolean isVararg;
    private final int index;

    public ValueParameterDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            int index,
            @NotNull List<Attribute> attributes,
            @NotNull String name,
            @Nullable JetType inType,
            @NotNull JetType outType,
            boolean hasDefaultValue,
            boolean isVararg) {
        super(containingDeclaration, attributes, name, inType, outType);
        this.index = index;
        this.hasDefaultValue = hasDefaultValue;
        this.isVararg = isVararg;
    }

    @Override
    public int getIndex() {
        return index;
//        final JetDeclaration element = getPsiElement();
//        final PsiElement parent = element.getParent();
//        if (parent instanceof JetParameterList) {
//            return ((JetParameterList) parent).getParameters().indexOf(element);
//        }
//        throw new IllegalStateException("couldn't find index for parameter");
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
