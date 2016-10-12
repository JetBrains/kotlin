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

package org.jetbrains.kotlin.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.resolve.scopes.TypeIntersectionScope;

import java.util.*;

public class IntersectionTypeConstructor implements TypeConstructor {
    private final Set<KotlinType> intersectedTypes;
    private final int hashCode;

    public IntersectionTypeConstructor(Collection<KotlinType> typesToIntersect) {
        assert !typesToIntersect.isEmpty() : "Attempt to create an empty intersection";

        this.intersectedTypes = new LinkedHashSet<KotlinType>(typesToIntersect);
        this.hashCode = intersectedTypes.hashCode();
    }

    @NotNull
    @Override
    public List<TypeParameterDescriptor> getParameters() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<KotlinType> getSupertypes() {
        return intersectedTypes;
    }

    public MemberScope createScopeForKotlinType() {
        return TypeIntersectionScope.create("member scope for intersection type " + this, intersectedTypes);
    }

    @Override
    public boolean isFinal() {
        return false;
    }

    @Override
    public boolean isDenotable() {
        return false;
    }

    @Override
    public ClassifierDescriptor getDeclarationDescriptor() {
        return null;
    }

    @NotNull
    @Override
    public KotlinBuiltIns getBuiltIns() {
        return intersectedTypes.iterator().next().getConstructor().getBuiltIns();
    }

    @Override
    public String toString() {
        return makeDebugNameForIntersectionType(intersectedTypes);
    }

    private static String makeDebugNameForIntersectionType(Iterable<KotlinType> resultingTypes) {
        StringBuilder debugName = new StringBuilder("{");
        for (Iterator<KotlinType> iterator = resultingTypes.iterator(); iterator.hasNext(); ) {
            KotlinType type = iterator.next();

            debugName.append(type.toString());
            if (iterator.hasNext()) {
                debugName.append(" & ");
            }
        }
        debugName.append("}");
        return debugName.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntersectionTypeConstructor that = (IntersectionTypeConstructor) o;

        if (intersectedTypes != null ? !intersectedTypes.equals(that.intersectedTypes) : that.intersectedTypes != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
