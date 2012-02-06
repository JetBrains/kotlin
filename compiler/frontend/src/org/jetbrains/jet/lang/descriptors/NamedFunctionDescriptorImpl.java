package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;

import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class NamedFunctionDescriptorImpl extends FunctionDescriptorImpl implements NamedFunctionDescriptor {

    public NamedFunctionDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull String name) {
        super(containingDeclaration, annotations, name);
    }

    private NamedFunctionDescriptorImpl(@NotNull NamedFunctionDescriptor original, @NotNull List<AnnotationDescriptor> annotations, @NotNull String name) {
        super(original, annotations, name);
    }

    @Override
    protected FunctionDescriptorImpl createSubstitutedCopy() {
        return new NamedFunctionDescriptorImpl(
                this,
                // TODO : safeSubstitute
                getAnnotations(),
                getName());
    }

    @NotNull
    @Override
    public NamedFunctionDescriptor copy(DeclarationDescriptor newOwner, boolean makeNonAbstract) {
        NamedFunctionDescriptorImpl copy = new NamedFunctionDescriptorImpl(newOwner, Lists.newArrayList(getAnnotations()), getName());
        copy.initialize(
                getReceiverParameter().exists() ? getReceiverParameter().getType() : null,
                expectedThisObject,
                DescriptorUtils.copyTypeParameters(copy, typeParameters),
                DescriptorUtils.copyValueParameters(copy, unsubstitutedValueParameters),
                unsubstitutedReturnType,
                DescriptorUtils.convertModality(modality, makeNonAbstract),
                visibility
        );
        return copy;
    }
}
