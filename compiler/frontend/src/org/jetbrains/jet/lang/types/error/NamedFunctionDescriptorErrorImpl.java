package org.jetbrains.jet.lang.types.error;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.NamedFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.NamedFunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.types.ErrorUtils;

import java.util.Collections;

/**
 * @author Stepan Koltsov
 */
public class NamedFunctionDescriptorErrorImpl extends NamedFunctionDescriptorImpl {
    // used for diagnostic only
    @NotNull
    private final ErrorUtils.ErrorScope ownerScope;

    public NamedFunctionDescriptorErrorImpl(ErrorUtils.ErrorScope ownerScope) {
        super(ErrorUtils.getErrorClass(), Collections.<AnnotationDescriptor>emptyList(), "<ERROR FUNCTION>", Kind.DECLARATION);
        this.ownerScope = ownerScope;
    }

    @Override
    protected FunctionDescriptorImpl createSubstitutedCopy(DeclarationDescriptor newOwner, boolean preserveOriginal, Kind kind) {
        return this;
    }

    @NotNull
    @Override
    public NamedFunctionDescriptor copy(DeclarationDescriptor newOwner, boolean makeNonAbstract, Kind kind, boolean copyOverrides) {
        return this;
    }
}
