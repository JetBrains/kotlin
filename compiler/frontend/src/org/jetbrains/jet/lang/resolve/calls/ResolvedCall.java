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
    @NotNull
    ResolutionStatus getStatus();

    @NotNull
    D getCandidateDescriptor();

    @NotNull
    D getResultingDescriptor();

    @NotNull
    ReceiverDescriptor getReceiverArgument();

    @NotNull
    ReceiverDescriptor getThisObject();

    @NotNull
    Map<ValueParameterDescriptor, ResolvedValueArgument> getValueArguments();

    Map<TypeParameterDescriptor, JetType> getTypeArguments();
}
