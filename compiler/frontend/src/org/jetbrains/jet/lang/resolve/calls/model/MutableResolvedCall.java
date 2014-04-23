/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve.calls.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.jet.lang.resolve.calls.results.ResolutionStatus;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collection;
import java.util.Set;

public interface MutableResolvedCall<D extends CallableDescriptor> extends ResolvedCall<D>  {

    @NotNull
    ResolutionStatus getStatus();

    void addStatus(@NotNull ResolutionStatus status);

    void setStatusToSuccess();

    /**
     * Resolved call can have incomplete type parameters
     * if ResolutionStatus is INCOMPLETE_TYPE_INFERENCE (might be completed successfully)
     * or OTHER_ERROR (cannot be completed successfully, but if there's only one candidate, should be completed anyway).
     * @return true if resolved call has unknown type parameters (inference is incomplete)
     */
    //todo remove, replace with "typeArguments.isEmpty() && !typeParameters.isEmpty()"
    boolean hasIncompleteTypeParameters();

    void setHasIncompleteTypeParameters(boolean hasIncompleteTypeParameters);

    //todo remove: use value to parameter map status
    boolean isDirty();

    void argumentHasNoType();

    @NotNull
    DelegatingBindingTrace getTrace();

    /**
     * //todo get rid of Impl class in the interface
     * If the call has incomplete type parameters, it should be completed after resolution process is finished.
     * Returns this for normal call and function call for VariableAsFunctionCall.
     */
    @NotNull
    ResolvedCallImpl<D> getCallToCompleteTypeArgumentInference();

    void markCallAsCompleted();

    boolean isCompleted();


    void recordValueArgument(@NotNull ValueParameterDescriptor valueParameter, @NotNull ResolvedValueArgument valueArgument);

    void recordArgumentMatch(@NotNull ValueArgument valueArgument, @NotNull ValueParameterDescriptor parameter, boolean hasTypeMismatch);

    @NotNull
    Set<ValueArgument> getUnmappedArguments();

    void addUnmappedArguments(@NotNull Collection<? extends ValueArgument> unmappedArguments);

    @Override
    @NotNull
    MutableDataFlowInfoForArguments getDataFlowInfoForArguments();

    void setInitialDataFlowInfo(@NotNull DataFlowInfo info);


    @Nullable
    ConstraintSystem getConstraintSystem();

    void setConstraintSystem(@NotNull ConstraintSystem constraintSystem);

    void setResultingSubstitutor(@NotNull TypeSubstitutor substitutor);

    //todo remove: use value to parameter map status
    boolean hasInferredReturnType();
}
