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

package org.jetbrains.jet.lang.types.checker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;

public class JetTypeChecker {

    public interface TypeConstructorEquality {
        boolean equals(@NotNull TypeConstructor a, @NotNull TypeConstructor b);
    }

    public static final JetTypeChecker DEFAULT = new JetTypeChecker(new TypeCheckingProcedure(new TypeCheckerTypingConstraints()));

    public static final JetTypeChecker FLEXIBLE_UNEQUAL_TO_INFLEXIBLE = new JetTypeChecker(new TypeCheckingProcedure(new TypeCheckerTypingConstraints()) {
        @Override
        protected boolean heterogeneousEquivalence(JetType inflexibleType, JetType flexibleType) {
            return false;
        }
    });

    @NotNull
    public static JetTypeChecker withAxioms(@NotNull final TypeConstructorEquality equalityAxioms) {
        return new JetTypeChecker(new TypeCheckingProcedure(new TypeCheckerTypingConstraints() {
            @Override
            public boolean assertEqualTypeConstructors(@NotNull TypeConstructor constructor1, @NotNull TypeConstructor constructor2) {
                return constructor1.equals(constructor2) || equalityAxioms.equals(constructor1, constructor2);
            }
        }));
    }

    private final TypeCheckingProcedure procedure;

    private JetTypeChecker(@NotNull TypeCheckingProcedure procedure) {
        this.procedure = procedure;
    }

    public boolean isSubtypeOf(@NotNull JetType subtype, @NotNull JetType supertype) {
        return procedure.isSubtypeOf(subtype, supertype);
    }

    public boolean equalTypes(@NotNull JetType a, @NotNull JetType b) {
        return procedure.equalTypes(a, b);
    }
}
