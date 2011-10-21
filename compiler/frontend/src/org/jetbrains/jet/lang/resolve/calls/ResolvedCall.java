package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Map;

/**
 * @author abreslav
 */
public interface ResolvedCall<D extends CallableDescriptor> {
    /** A target callable descriptor as it was accessible in the corresponding scope, i.e. with type parameters not substituted */
    @NotNull
    D getCandidateDescriptor();

    /** Type parameters are substituted */
    @NotNull
    D getResultingDescriptor();

    /** If the target was an extension function or property, this is the value for its receiver parameter */
    @NotNull
    ReceiverDescriptor getReceiverArgument();

    /** If the target was a member of a class, this is the object of that class to call it on */
    @NotNull
    ReceiverDescriptor getThisObject();

    /** Values (arguments) for value parameters */
    @NotNull
    Map<ValueParameterDescriptor, ResolvedValueArgument> getValueArguments();

    /** What's substituted for type parameters */
    @NotNull
    Map<TypeParameterDescriptor, JetType> getTypeArguments();
}
