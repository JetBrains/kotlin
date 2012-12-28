/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.calls.inference;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

import java.util.List;

public class InferenceErrorData {
    public final CallableDescriptor descriptor;
    public final ConstraintSystem constraintSystem;

    private InferenceErrorData(@NotNull CallableDescriptor descriptor, @NotNull ConstraintSystem constraintSystem) {
        this.descriptor = descriptor;
        this.constraintSystem = constraintSystem;
    }

    public static class ExtendedInferenceErrorData extends InferenceErrorData {
        public final JetType receiverArgumentType;
        public final JetType expectedType;

        public final List<JetType> valueArgumentsTypes;

        private ExtendedInferenceErrorData(
                @NotNull CallableDescriptor descriptor, @NotNull ConstraintSystem constraintSystem,
                @NotNull List<JetType> valueArgumentsTypes, @Nullable JetType receiverArgumentType, @Nullable JetType expectedType
        ) {
            super(descriptor, constraintSystem);
            this.receiverArgumentType = receiverArgumentType;
            this.valueArgumentsTypes = valueArgumentsTypes;
            this.expectedType = expectedType;
        }
    }

    public static ExtendedInferenceErrorData create(@NotNull CallableDescriptor descriptor, @NotNull ConstraintSystem constraintSystem,
            @NotNull List<JetType> valueArgumentsTypes, @Nullable JetType receiverArgumentType, @Nullable JetType expectedType) {
        return new ExtendedInferenceErrorData(descriptor, constraintSystem, valueArgumentsTypes, receiverArgumentType,
                                      expectedType != TypeUtils.NO_EXPECTED_TYPE ? expectedType : null);
    }

    public static InferenceErrorData create(@NotNull CallableDescriptor descriptor, @NotNull ConstraintSystem constraintSystem) {
        return new InferenceErrorData(descriptor, constraintSystem);
    }
}