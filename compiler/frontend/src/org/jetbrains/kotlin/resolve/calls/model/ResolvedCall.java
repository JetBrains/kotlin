/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.psi.Call;
import org.jetbrains.kotlin.psi.ValueArgument;
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus;
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.List;
import java.util.Map;

public interface ResolvedCall<D extends CallableDescriptor> {
    @NotNull
    ResolutionStatus getStatus();

    /** The call that was resolved to this ResolvedCall */
    @NotNull
    Call getCall();

    /** A target callable descriptor as it was accessible in the corresponding scope, i.e. with type arguments not substituted */
    @NotNull
    D getCandidateDescriptor();

    /** Type arguments are substituted. This descriptor is guaranteed to have NO declared type parameters */
    @NotNull
    D getResultingDescriptor();

    /** If the target was an extension function or property, this is the value for its receiver parameter */
    @Nullable
    ReceiverValue getExtensionReceiver();

    /** If the target was a member of a class, this is the object of that class to call it on */
    @Nullable
    ReceiverValue getDispatchReceiver();

    /** Determines whether receiver argument or this object is substituted for explicit receiver */
    @NotNull
    ExplicitReceiverKind getExplicitReceiverKind();

    /** Values (arguments) for value parameters */
    @NotNull
    Map<ValueParameterDescriptor, ResolvedValueArgument> getValueArguments();

    /** Values (arguments) for value parameters indexed by parameter index */
    @Nullable
    List<ResolvedValueArgument> getValueArgumentsByIndex();

    /** The result of mapping the value argument to a parameter */
    @NotNull
    ArgumentMapping getArgumentMapping(@NotNull ValueArgument valueArgument);

    /** What's substituted for type parameters */
    @NotNull
    Map<TypeParameterDescriptor, KotlinType> getTypeArguments();

    /** Data flow info for each argument and the result data flow info */
    @NotNull
    DataFlowInfoForArguments getDataFlowInfoForArguments();

    @Nullable
    KotlinType getSmartCastDispatchReceiverType();
}
