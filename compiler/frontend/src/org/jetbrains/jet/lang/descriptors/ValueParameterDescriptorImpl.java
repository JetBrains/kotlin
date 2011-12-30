package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.List;

/**
 * @author abreslav
 */
public class ValueParameterDescriptorImpl extends VariableDescriptorImpl implements MutableValueParameterDescriptor {
    private final boolean hasDefaultValue;
    private final JetType varargElementType;
    private final boolean isVar;
    private final int index;
    private final ValueParameterDescriptor original;

    public ValueParameterDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            int index,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull String name,
            boolean isVar,
            @NotNull JetType outType,
            boolean hasDefaultValue,
            @Nullable JetType varargElementType) {
        super(containingDeclaration, annotations, name, outType);
        this.original = this;
        this.index = index;
        this.hasDefaultValue = hasDefaultValue;
        this.varargElementType = varargElementType;
        this.isVar = isVar;
    }

    public ValueParameterDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull ValueParameterDescriptor original,
            @NotNull List<AnnotationDescriptor> annotations,
            boolean isVar,
            @NotNull JetType outType,
            @Nullable JetType varargElementType
            ) {
        super(containingDeclaration, annotations, original.getName(), outType);
        this.original = original;
        this.index = original.getIndex();
        this.hasDefaultValue = original.hasDefaultValue();
        this.varargElementType = varargElementType;
        this.isVar = isVar;
    }

    @Override
    public void setType(@NotNull JetType type) {
        setOutType(type);
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public boolean hasDefaultValue() {
        return hasDefaultValue;
    }

    @Override
    public boolean isRef() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Nullable
    public JetType getVarargElementType() {
        return varargElementType;
    }

    @NotNull
    @Override
    public ValueParameterDescriptor getOriginal() {
        return original == this ? this : original.getOriginal();
    }

    @NotNull
    @Override
    public ValueParameterDescriptor substitute(TypeSubstitutor substitutor) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitValueParameterDescriptor(this, data);
    }

    @Override
    public boolean isVar() {
        return isVar;
    }

    @NotNull
    @Override
    public ValueParameterDescriptor copy(@NotNull DeclarationDescriptor newOwner) {
        return new ValueParameterDescriptorImpl(newOwner, index, Lists.newArrayList(getAnnotations()), getName(), isVar, getOutType(), hasDefaultValue, varargElementType);
    }
}
