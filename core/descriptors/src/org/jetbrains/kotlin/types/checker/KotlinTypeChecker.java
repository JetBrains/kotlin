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

public interface KotlinTypeChecker {

    interface TypeConstructorEquality {
        boolean equals(@NotNull TypeConstructor a, @NotNull TypeConstructor b);
    }

    KotlinTypeChecker DEFAULT = new KotlinTypeCheckerImpl(new TypeCheckingProcedure(new TypeCheckerProcedureCallbacksImpl()));

    KotlinTypeChecker ERROR_TYPES_ARE_EQUAL_TO_ANYTHING = new KotlinTypeCheckerImpl(new TypeCheckingProcedure(new TypeCheckerProcedureCallbacksImpl() {
        @Override
        public boolean assertEqualTypes(@NotNull KotlinType a, @NotNull KotlinType b, @NotNull TypeCheckingProcedure typeCheckingProcedure) {
            return a.isError() || b.isError() || super.assertEqualTypes(a, b, typeCheckingProcedure);
        }
    }));

    KotlinTypeChecker FLEXIBLE_UNEQUAL_TO_INFLEXIBLE = new KotlinTypeCheckerImpl(new TypeCheckingProcedure(new TypeCheckerProcedureCallbacksImpl()) {
        @Override
        protected boolean heterogeneousEquivalence(KotlinType inflexibleType, KotlinType flexibleType) {
            return false;
        }
    });


    boolean isSubtypeOf(@NotNull KotlinType subtype, @NotNull KotlinType supertype);
    boolean equalTypes(@NotNull KotlinType a, @NotNull KotlinType b);
}
