/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.psi.ValueArgument;
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace;
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystem;
import org.jetbrains.kotlin.resolve.calls.results.ResolutionStatus;
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeSubstitutor;

public interface MutableResolvedCall<D extends CallableDescriptor> extends ResolvedCall<D>  {

    void addStatus(@NotNull ResolutionStatus status);

    void setStatusToSuccess();

    @NotNull
    DelegatingBindingTrace getTrace();

    @NotNull
    TracingStrategy getTracingStrategy();

    void markCallAsCompleted();

    boolean isCompleted();


    void recordValueArgument(@NotNull ValueParameterDescriptor valueParameter, @NotNull ResolvedValueArgument valueArgument);

    void recordArgumentMatchStatus(@NotNull ValueArgument valueArgument, @NotNull ArgumentMatchStatus matchStatus);

    @Override
    @NotNull
    MutableDataFlowInfoForArguments getDataFlowInfoForArguments();

    @Nullable
    ConstraintSystem getConstraintSystem();

    void setConstraintSystem(@NotNull ConstraintSystem constraintSystem);

    void setResultingSubstitutor(@NotNull TypeSubstitutor substitutor);

    @Nullable
    TypeSubstitutor getKnownTypeParametersSubstitutor();

    //todo remove: use value to parameter map status
    boolean hasInferredReturnType();

    void setSmartCastDispatchReceiverType(@NotNull KotlinType smartCastDispatchReceiverType);

    void updateExtensionReceiverWithSmartCastIfNeeded(@NotNull KotlinType smartCastExtensionReceiverType);
}
