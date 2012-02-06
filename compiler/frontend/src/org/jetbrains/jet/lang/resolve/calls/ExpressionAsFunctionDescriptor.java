package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collections;
import java.util.List;

/**
 * @author alex.tkachman
 */
public class ExpressionAsFunctionDescriptor extends FunctionDescriptorImpl {
    public ExpressionAsFunctionDescriptor(DeclarationDescriptor containingDeclaration, String name) {
        super(containingDeclaration, Collections.<AnnotationDescriptor>emptyList(), name);
    }

    @Override
    protected FunctionDescriptorImpl createSubstitutedCopy(DeclarationDescriptor newOwner, boolean preserveOriginal) {
        throw new IllegalStateException();
    }

    @NotNull
    @Override
    public FunctionDescriptor copy(DeclarationDescriptor newOwner, boolean makeNonAbstract) {
        throw new IllegalStateException();
    }
}
