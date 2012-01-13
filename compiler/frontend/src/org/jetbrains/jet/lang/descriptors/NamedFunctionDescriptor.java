package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;

/**
 * @author Stepan Koltsov
 */
public interface NamedFunctionDescriptor extends FunctionDescriptor {

    @NotNull
    @Override
    NamedFunctionDescriptor copy(DeclarationDescriptor newOwner, boolean makeNonAbstract);
}
