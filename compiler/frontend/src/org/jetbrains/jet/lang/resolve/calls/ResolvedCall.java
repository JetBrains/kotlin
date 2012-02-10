package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;

import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public interface ResolvedCall<D extends CallableDescriptor> {
    /** A target callable descriptor as it was accessible in the corresponding scope, i.e. with type arguments not substituted */
    @NotNull
    D getCandidateDescriptor();

    /** Type arguments are substituted. This descriptor is guaranteed to have NO declared type parameters */
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

    /** Values (arguments) for value parameters indexed by parameter index */
    @NotNull
    List<ResolvedValueArgument> getValueArgumentsByIndex();

    /** What's substituted for type parameters */
    @NotNull
    Map<TypeParameterDescriptor, JetType> getTypeArguments();
}
