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

package org.jetbrains.kotlin.resolve.calls.inference;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.psi.Call;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.List;

public class InferenceErrorData {
    @NotNull
    public final CallableDescriptor descriptor;
    @NotNull
    public final ConstraintSystem constraintSystem;
    @Nullable
    public final KotlinType receiverArgumentType;
    @NotNull
    public final KotlinType expectedType;
    @NotNull
    public final List<KotlinType> valueArgumentsTypes;
    @NotNull
    public final Call call;

    private InferenceErrorData(
            @NotNull CallableDescriptor descriptor,
            @NotNull ConstraintSystem constraintSystem,
            @NotNull List<KotlinType> valueArgumentsTypes,
            @Nullable KotlinType receiverArgumentType,
            @NotNull KotlinType expectedType,
            @NotNull Call call
    ) {
        this.descriptor = descriptor;
        this.constraintSystem = constraintSystem;
        this.receiverArgumentType = receiverArgumentType;
        this.valueArgumentsTypes = valueArgumentsTypes;
        this.expectedType = expectedType;
        this.call = call;
    }

    @NotNull
    public static InferenceErrorData create(
            @NotNull CallableDescriptor descriptor,
            @NotNull ConstraintSystem constraintSystem,
            @NotNull List<KotlinType> valueArgumentsTypes,
            @Nullable KotlinType receiverArgumentType,
            @NotNull KotlinType expectedType,
            @NotNull Call call
    ) {
        return new InferenceErrorData(descriptor, constraintSystem, valueArgumentsTypes, receiverArgumentType, expectedType, call);
    }
}
