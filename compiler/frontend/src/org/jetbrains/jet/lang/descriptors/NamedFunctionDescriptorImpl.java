package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class NamedFunctionDescriptorImpl extends FunctionDescriptorImpl implements NamedFunctionDescriptor {

    public NamedFunctionDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull List<AnnotationDescriptor> annotations, @NotNull String name) {
        super(containingDeclaration, annotations, name);
    }

    private NamedFunctionDescriptorImpl(@NotNull DeclarationDescriptor containingDeclaration, @NotNull NamedFunctionDescriptor original, @NotNull List<AnnotationDescriptor> annotations, @NotNull String name) {
        super(containingDeclaration, original, annotations, name);
    }

    @NotNull
    @Override
    public NamedFunctionDescriptor getOriginal() {
        return (NamedFunctionDescriptor) super.getOriginal();
    }

    @Override
    protected FunctionDescriptorImpl createSubstitutedCopy(DeclarationDescriptor newOwner, boolean preserveOriginal) {
        if (preserveOriginal) {
            return new NamedFunctionDescriptorImpl(
                    newOwner,
                    getOriginal(),
                    // TODO : safeSubstitute
                    getAnnotations(),
                    getName());
        } else {
            return new NamedFunctionDescriptorImpl(
                    newOwner,
                    // TODO : safeSubstitute
                    getAnnotations(),
                    getName());
        }
    }

    @NotNull
    @Override
    public NamedFunctionDescriptor copy(DeclarationDescriptor newOwner, boolean makeNonAbstract) {
        return (NamedFunctionDescriptor) doSubstitute(TypeSubstitutor.EMPTY, newOwner, DescriptorUtils.convertModality(modality, makeNonAbstract), false);
    }
}
