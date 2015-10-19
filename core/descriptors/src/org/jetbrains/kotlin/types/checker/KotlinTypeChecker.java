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

package org.jetbrains.kotlin.types.checker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.types.KtType;
import org.jetbrains.kotlin.types.TypeConstructor;

public class KotlinTypeChecker {

    public interface TypeConstructorEquality {
        boolean equals(@NotNull TypeConstructor a, @NotNull TypeConstructor b);
    }

    public static final KotlinTypeChecker DEFAULT = new KotlinTypeChecker(new TypeCheckingProcedure(new TypeCheckerProcedureCallbacksImpl()));

    public static final KotlinTypeChecker ERROR_TYPES_ARE_EQUAL_TO_ANYTHING = new KotlinTypeChecker(new TypeCheckingProcedure(new TypeCheckerProcedureCallbacksImpl() {
        @Override
        public boolean assertEqualTypes(@NotNull KtType a, @NotNull KtType b, @NotNull TypeCheckingProcedure typeCheckingProcedure) {
            return a.isError() || b.isError() || super.assertEqualTypes(a, b, typeCheckingProcedure);
        }
    }));

    public static final KotlinTypeChecker FLEXIBLE_UNEQUAL_TO_INFLEXIBLE = new KotlinTypeChecker(new TypeCheckingProcedure(new TypeCheckerProcedureCallbacksImpl()) {
        @Override
        protected boolean heterogeneousEquivalence(KtType inflexibleType, KtType flexibleType) {
            return false;
        }
    });

    @NotNull
    public static KotlinTypeChecker withAxioms(@NotNull final TypeConstructorEquality equalityAxioms) {
        return new KotlinTypeChecker(new TypeCheckingProcedure(new TypeCheckerProcedureCallbacksImpl() {
            @Override
            public boolean assertEqualTypeConstructors(@NotNull TypeConstructor constructor1, @NotNull TypeConstructor constructor2) {
                return constructor1.equals(constructor2) || equalityAxioms.equals(constructor1, constructor2);
            }
        }));
    }

    private final TypeCheckingProcedure procedure;

    private KotlinTypeChecker(@NotNull TypeCheckingProcedure procedure) {
        this.procedure = procedure;
    }

    public boolean isSubtypeOf(@NotNull KtType subtype, @NotNull KtType supertype) {
        return procedure.isSubtypeOf(subtype, supertype);
    }

    public boolean equalTypes(@NotNull KtType a, @NotNull KtType b) {
        return procedure.equalTypes(a, b);
    }
}
