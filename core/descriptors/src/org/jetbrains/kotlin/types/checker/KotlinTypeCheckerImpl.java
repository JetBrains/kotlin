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

package org.jetbrains.kotlin.types.checker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeConstructor;

public class KotlinTypeCheckerImpl implements KotlinTypeChecker {


    @NotNull
    public static KotlinTypeChecker withAxioms(@NotNull final TypeConstructorEquality equalityAxioms) {
        return new KotlinTypeCheckerImpl(new TypeCheckingProcedure(new TypeCheckerProcedureCallbacksImpl() {
            @Override
            public boolean assertEqualTypeConstructors(@NotNull TypeConstructor constructor1, @NotNull TypeConstructor constructor2) {
                return constructor1.equals(constructor2) || equalityAxioms.equals(constructor1, constructor2);
            }
        }));
    }

    private final TypeCheckingProcedure procedure;

    protected KotlinTypeCheckerImpl(@NotNull TypeCheckingProcedure procedure) {
        this.procedure = procedure;
    }

    @Override
    public boolean isSubtypeOf(@NotNull KotlinType subtype, @NotNull KotlinType supertype) {
        return procedure.isSubtypeOf(subtype, supertype);
    }

    @Override
    public boolean equalTypes(@NotNull KotlinType a, @NotNull KotlinType b) {
        return procedure.equalTypes(a, b);
    }

}
