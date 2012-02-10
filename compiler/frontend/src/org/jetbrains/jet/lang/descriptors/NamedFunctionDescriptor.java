package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;

/**
 * ... and also a closure
 *
 * @author Stepan Koltsov
 */
public interface NamedFunctionDescriptor extends FunctionDescriptor {

    @NotNull
    @Override
    NamedFunctionDescriptor copy(DeclarationDescriptor newOwner, boolean makeNonAbstract, Kind kind, boolean copyOverrides);

    @NotNull
    @Override
    NamedFunctionDescriptor getOriginal();
}
