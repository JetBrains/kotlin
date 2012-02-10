package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;

import java.util.Collections;

/**
 * @author alex.tkachman
 */
public class ExpressionAsFunctionDescriptor extends FunctionDescriptorImpl {
    public ExpressionAsFunctionDescriptor(DeclarationDescriptor containingDeclaration, String name) {
        super(containingDeclaration, Collections.<AnnotationDescriptor>emptyList(), name, Kind.DECLARATION);
    }

    @Override
    protected FunctionDescriptorImpl createSubstitutedCopy(DeclarationDescriptor newOwner, boolean preserveOriginal, Kind kind) {
        throw new IllegalStateException();
    }

    @NotNull
    @Override
    public FunctionDescriptor copy(DeclarationDescriptor newOwner, boolean makeNonAbstract, Kind kind, boolean copyOverrides) {
        throw new IllegalStateException();
    }
}
