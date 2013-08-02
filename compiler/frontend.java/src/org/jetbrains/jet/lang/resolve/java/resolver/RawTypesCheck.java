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

import com.intellij.psi.HierarchicalMethodSignature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.structure.*;

public class RawTypesCheck {
    private static boolean isPartiallyRawType(@NotNull JavaType type) {
        if (type instanceof JavaPrimitiveType) {
            return false;
        }
        else if (type instanceof JavaClassifierType) {
            JavaClassifierType classifierType = (JavaClassifierType) type;

            if (classifierType.isRaw()) {
                return true;
            }

            for (JavaType argument : classifierType.getTypeArguments()) {
                if (isPartiallyRawType(argument)) {
                    return true;
                }
            }

            return false;
        }
        else if (type instanceof JavaArrayType) {
            return isPartiallyRawType(((JavaArrayType) type).getComponentType());
        }
        else if (type instanceof JavaWildcardType) {
            JavaType bound = ((JavaWildcardType) type).getBound();
            return bound != null && isPartiallyRawType(bound);
        }
        else {
            throw new IllegalStateException("Unexpected type: " + type);
        }
    }

    private static boolean hasRawTypesInSignature(@NotNull JavaMethod method) {
        JavaType returnType = method.getReturnType();
        if (returnType != null && isPartiallyRawType(returnType)) {
            return true;
        }

        for (JavaValueParameter parameter : method.getValueParameters()) {
            if (isPartiallyRawType(parameter.getType())) {
                return true;
            }
        }

        for (JavaTypeParameter typeParameter : method.getTypeParameters()) {
            for (JavaClassifierType upperBound : typeParameter.getUpperBounds()) {
                if (isPartiallyRawType(upperBound)) {
                    return true;
                }
            }
        }

        return false;
    }

    static boolean hasRawTypesInHierarchicalSignature(@NotNull JavaMethod method) {
        // This is a very important optimization: package-classes are big and full of static methods
        // building method hierarchies for such classes takes a very long time
        if (method.isStatic()) return false;

        if (hasRawTypesInSignature(method)) {
            return true;
        }

        for (HierarchicalMethodSignature superSignature : method.getPsi().getHierarchicalMethodSignature().getSuperSignatures()) {
            JavaMethod superMethod = new JavaMethod(superSignature.getMethod());
            if (superSignature.isRaw() || typeParameterIsErased(method, superMethod) || hasRawTypesInSignature(superMethod)) {
                return true;
            }
        }

        return false;
    }

    private static boolean typeParameterIsErased(@NotNull JavaMethod method, @NotNull JavaMethod superMethod) {
        // Java allows you to write
        //   <T extends Foo> T foo(), in the superclass and then
        //   Foo foo(), in the subclass
        // this is a valid Java override, but in fact it is an erasure
        return method.getTypeParameters().size() != superMethod.getTypeParameters().size();
    }

    private RawTypesCheck() {
    }
}
