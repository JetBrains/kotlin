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

package org.jetbrains.jet.lang.resolve.java.resolver;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RawTypesCheck {
    private static boolean isPartiallyRawType(@NotNull PsiType type) {
        return type.accept(new PsiTypeVisitor<Boolean>() {
            @Nullable
            @Override
            public Boolean visitPrimitiveType(PsiPrimitiveType primitiveType) {
                return false;
            }

            @Nullable
            @Override
            public Boolean visitClassType(PsiClassType classType) {
                if (classType.isRaw()) {
                    return true;
                }

                for (PsiType argument : classType.getParameters()) {
                    if (argument.accept(this)) {
                        return true;
                    }
                }

                return false;
            }

            @Nullable
            @Override
            public Boolean visitArrayType(PsiArrayType arrayType) {
                return arrayType.getComponentType().accept(this);
            }

            @Nullable
            @Override
            public Boolean visitWildcardType(PsiWildcardType wildcardType) {
                PsiType bound = wildcardType.getBound();
                return bound == null ? false : bound.accept(this);
            }

            @Nullable
            @Override
            public Boolean visitType(PsiType type) {
                throw new IllegalStateException(type.getClass().getSimpleName() + " is unexpected");
            }
        });
    }

    private static boolean hasRawTypesInSignature(@NotNull PsiMethod method) {
        PsiType returnType = method.getReturnType();
        if (returnType != null && isPartiallyRawType(returnType)) {
            return true;
        }

        for (PsiParameter parameter : method.getParameterList().getParameters()) {
            if (isPartiallyRawType(parameter.getType())) {
                return true;
            }
        }

        for (PsiTypeParameter typeParameter : method.getTypeParameters()) {
            for (PsiClassType upperBound : typeParameter.getExtendsList().getReferencedTypes()) {
                if (isPartiallyRawType(upperBound)) {
                    return true;
                }
            }
        }

        return false;
    }

    static boolean hasRawTypesInHierarchicalSignature(@NotNull PsiMethod method) {
        // This is a very important optimization: package-classes are big and full of static methods
        // building method hierarchies for such classes takes a very long time
        if (method.hasModifierProperty(PsiModifier.STATIC)) return false;

        if (hasRawTypesInSignature(method)) {
            return true;
        }

        for (HierarchicalMethodSignature superSignature : method.getHierarchicalMethodSignature().getSuperSignatures()) {
            PsiMethod superMethod = superSignature.getMethod();
            if (superSignature.isRaw() || typeParameterIsErased(method, superMethod) || hasRawTypesInSignature(superMethod)) {
                return true;
            }
        }

        return false;
    }

    private static boolean typeParameterIsErased(@NotNull PsiMethod a, @NotNull PsiMethod b) {
        // Java allows you to write
        //   <T extends Foo> T foo(), in the superclass and then
        //   Foo foo(), in the subclass
        // this is a valid Java override, but in fact it is an erasure
        return a.getTypeParameters().length != b.getTypeParameters().length;
    }

    private RawTypesCheck() {
    }
}
