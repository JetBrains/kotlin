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

package org.jetbrains.kotlin.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.types.*;

/**
 * Bare types are somewhat like raw types, but in Kotlin they are only allowed on the right-hand side of is/as.
 * For example:
 *
 *   fun foo(a: Any) {
 *     if (a is List) {
 *       // a is known to be List<*> here
 *     }
 *   }
 *
 * Another example:
 *
 *   fun foo(a: Collection<String>) {
 *     if (a is List) {
 *       // a is known to be List<String> here
 *     }
 *   }
 *
 * One can call reconstruct(supertype) to get an actual type from a bare type
 */
public class PossiblyBareType {

    @NotNull
    public static PossiblyBareType bare(@NotNull TypeConstructor bareTypeConstructor, boolean nullable) {
        return new PossiblyBareType(null, bareTypeConstructor, nullable);
    }

    @NotNull
    public static PossiblyBareType type(@NotNull KotlinType actualType) {
        return new PossiblyBareType(actualType, null, false);
    }

    private final KotlinType actualType;
    private final TypeConstructor bareTypeConstructor;
    private final boolean nullable;

    private PossiblyBareType(@Nullable KotlinType actualType, @Nullable TypeConstructor bareTypeConstructor, boolean nullable) {
        this.actualType = actualType;
        this.bareTypeConstructor = bareTypeConstructor;
        this.nullable = nullable;
    }

    public boolean isBare() {
        return actualType == null;
    }

    @NotNull
    public KotlinType getActualType() {
        //noinspection ConstantConditions
        return actualType;
    }

    @NotNull
    public TypeConstructor getBareTypeConstructor() {
        //noinspection ConstantConditions
        return bareTypeConstructor;
    }

    private boolean isBareTypeNullable() {
        return nullable;
    }

    public boolean isNullable() {
        if (isBare()) return isBareTypeNullable();
        return getActualType().isMarkedNullable();
    }

    public PossiblyBareType makeNullable() {
        if (isBare()) {
            return isBareTypeNullable() ? this : bare(getBareTypeConstructor(), true);
        }

        return type(TypeUtils.makeNullable(getActualType()));
    }

    @NotNull
    public TypeReconstructionResult reconstruct(@NotNull KotlinType subjectType) {
        if (!isBare()) return new TypeReconstructionResult(getActualType(), true);

        TypeReconstructionResult reconstructionResult = CastDiagnosticsUtil.findStaticallyKnownSubtype(
                TypeUtils.makeNotNullable(subjectType),
                getBareTypeConstructor()
        );
        KotlinType type = reconstructionResult.getResultingType();
        // No need to make an absent type nullable
        if (type == null) return reconstructionResult;

        KotlinType resultingType = TypeUtils.makeNullableAsSpecified(type, isBareTypeNullable());
        return new TypeReconstructionResult(resultingType, reconstructionResult.isAllArgumentsInferred());
    }

    @Override
    public String toString() {
        return isBare() ? "bare " + bareTypeConstructor + (isBareTypeNullable() ? "?" : "") : getActualType().toString();
    }
}
