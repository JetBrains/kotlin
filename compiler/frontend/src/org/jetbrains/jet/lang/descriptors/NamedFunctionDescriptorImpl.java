package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class NamedFunctionDescriptorImpl extends FunctionDescriptorImpl implements NamedFunctionDescriptor {

    public NamedFunctionDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull String name,
            Kind kind) {
        super(containingDeclaration, annotations, name, kind);
    }

    private NamedFunctionDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull NamedFunctionDescriptor original,
            @NotNull List<AnnotationDescriptor> annotations,
            @NotNull String name,
            Kind kind) {
        super(containingDeclaration, original, annotations, name, kind);
    }

    @NotNull
    @Override
    public NamedFunctionDescriptor getOriginal() {
        return (NamedFunctionDescriptor) super.getOriginal();
    }

    @Override
    protected FunctionDescriptorImpl createSubstitutedCopy(DeclarationDescriptor newOwner, boolean preserveOriginal, Kind kind) {
        if (preserveOriginal) {
            return new NamedFunctionDescriptorImpl(
                    newOwner,
                    getOriginal(),
                    // TODO : safeSubstitute
                    getAnnotations(),
                    getName(),
                    kind);
        } else {
            return new NamedFunctionDescriptorImpl(
                    newOwner,
                    // TODO : safeSubstitute
                    getAnnotations(),
                    getName(),
                    kind);
        }
    }

    @NotNull
    @Override
    public NamedFunctionDescriptor copy(DeclarationDescriptor newOwner, boolean makeNonAbstract, Kind kind, boolean copyOverrides) {
        return (NamedFunctionDescriptor) doSubstitute(TypeSubstitutor.EMPTY, newOwner, DescriptorUtils.convertModality(modality, makeNonAbstract), false, copyOverrides, kind);
    }
}
