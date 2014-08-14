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

package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotatedImpl;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;

import java.util.*;

public class IntersectionTypeConstructor extends AnnotatedImpl implements TypeConstructor {
    private final Set<JetType> intersectedTypes;
    private final int hashCode;

    public IntersectionTypeConstructor(Annotations annotations, Collection<JetType> typesToIntersect) {
        super(annotations);
        this.intersectedTypes = new LinkedHashSet<JetType>(typesToIntersect);
        this.hashCode = intersectedTypes.hashCode();
    }

    @NotNull
    @Override
    public List<TypeParameterDescriptor> getParameters() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<JetType> getSupertypes() {
        return intersectedTypes;
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

    @Override
    public String toString() {
        return makeDebugNameForIntersectionType(intersectedTypes);
    }

    private static String makeDebugNameForIntersectionType(Iterable<JetType> resultingTypes) {
        StringBuilder debugName = new StringBuilder("{");
        for (Iterator<JetType> iterator = resultingTypes.iterator(); iterator.hasNext(); ) {
            JetType type = iterator.next();

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
