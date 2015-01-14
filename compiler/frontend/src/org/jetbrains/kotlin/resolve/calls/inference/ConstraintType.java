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
import org.jetbrains.kotlin.types.JetType;

import java.text.MessageFormat;

/**
 * A specific type for subtype constraint of types.
 */
public enum ConstraintType implements Comparable<ConstraintType> {
    // The order of these constants DOES matter
    // they are compared according to ordinal() values
    // First element has the highest priority
    RECEIVER("{0} is not a subtype of the expected receiver type {1}"),
    VALUE_ARGUMENT("Type mismatch: argument type is {0}, but {1} was expected"),
    EXPECTED_TYPE("Resulting type is {0} but {1} was expected"),
    PARAMETER_BOUND("Type parameter bound is not satisfied: {0} is not a subtype of {1}");

    private final String errorMessageTemplate; // {0} is subtype, {1} is supertype

    private ConstraintType(@NotNull String errorMessageTemplate) {
        this.errorMessageTemplate = errorMessageTemplate;
    }

    @NotNull
    public SubtypingConstraint assertSubtyping(@NotNull JetType subtype, @NotNull JetType supertype) {
        return new SubtypingConstraint(this, subtype, supertype);
    }

    @NotNull
    public String makeErrorMessage(@NotNull SubtypingConstraint constraint) {
        return MessageFormat.format(errorMessageTemplate, constraint.getSubtype(), constraint.getSupertype());
    }
}
