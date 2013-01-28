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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeConstructor;

public class JetTypeChecker {

    public static final JetTypeChecker INSTANCE = new JetTypeChecker();
    public static final HashBiMap<TypeConstructor, TypeConstructor> EMPTY_AXIOMS = HashBiMap.create();

    private JetTypeChecker() {
    }

    public boolean isSubtypeOf(@NotNull JetType subtype, @NotNull JetType supertype) {
//        return new TypeCheckingProcedure().run(subtype, supertype);
        return TYPE_CHECKER.isSubtypeOf(subtype, supertype);
    }

    public boolean equalTypes(@NotNull JetType a, @NotNull JetType b) {
        return TYPE_CHECKER.equalTypes(a, b);
    }

    public boolean equalTypes(@NotNull JetType a, @NotNull JetType b, @NotNull final BiMap<TypeConstructor, TypeConstructor> equalityAxioms) {
        return new TypeCheckingProcedure(new TypeCheckerTypingConstraints() {
            @Override
            public boolean assertEqualTypeConstructors(@NotNull TypeConstructor constructor1, @NotNull TypeConstructor constructor2) {
                if (!constructor1.equals(constructor2)) {
                    TypeConstructor img1 = equalityAxioms.get(constructor1);
                    TypeConstructor img2 = equalityAxioms.get(constructor2);
                    if (!(img1 != null && img1.equals(constructor2)) &&
                            !(img2 != null && img2.equals(constructor1))) {
                        return false;
                    }
                }
                return true;
            }
        }).equalTypes(a, b);
    }

    private static final TypeCheckingProcedure TYPE_CHECKER = new TypeCheckingProcedure(new TypeCheckerTypingConstraints());

    private static class TypeCheckerTypingConstraints implements TypingConstraints {
        @Override
        public boolean assertEqualTypes(@NotNull JetType a, @NotNull JetType b, @NotNull TypeCheckingProcedure typeCheckingProcedure) {
            return typeCheckingProcedure.equalTypes(a, b);
//            return TypeUtils.equalTypes(a, b);
        }

        @Override
        public boolean assertEqualTypeConstructors(@NotNull TypeConstructor a, @NotNull TypeConstructor b) {
            return a.equals(b);
        }

        @Override
        public boolean assertSubtype(@NotNull JetType subtype, @NotNull JetType supertype, @NotNull TypeCheckingProcedure typeCheckingProcedure) {
            return typeCheckingProcedure.isSubtypeOf(subtype, supertype);
        }

        @Override
        public boolean noCorrespondingSupertype(@NotNull JetType subtype, @NotNull JetType supertype) {
            return false; // type checking fails
        }
    }
}
