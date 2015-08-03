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

package org.jetbrains.kotlin.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;

import java.util.Iterator;
import java.util.List;

public abstract class AbstractJetType implements JetType {
    @Nullable
    @Override
    public <T extends TypeCapability> T getCapability(@NotNull Class<T> capabilityClass) {
        return getCapabilities().getCapability(capabilityClass);
    }

    @NotNull
    @Override
    public TypeCapabilities getCapabilities() {
        return TypeCapabilities.NONE.INSTANCE$;
    }

    @Override
    public final int hashCode() {
        int result = getConstructor().hashCode();
        result = 31 * result + getArguments().hashCode();
        result = 31 * result + (isMarkedNullable() ? 1 : 0);
        return result;
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JetType)) return false;

        JetType type = (JetType) obj;

        return isMarkedNullable() == type.isMarkedNullable() && JetTypeChecker.FLEXIBLE_UNEQUAL_TO_INFLEXIBLE.equalTypes(this, type);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (AnnotationDescriptor annotation : getAnnotations()) {
            sb.append("[");
            sb.append(DescriptorRenderer.DEBUG_TEXT.renderAnnotation(annotation));
            sb.append("] ");
        }

        sb.append(getConstructor());

        List<TypeProjection> arguments = getArguments();
        if (!arguments.isEmpty()) {
            sb.append("<");
            for (Iterator<TypeProjection> i = arguments.iterator(); i.hasNext(); ) {
                sb.append(i.next());
                if (i.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append(">");
        }

        if (isMarkedNullable()) {
            sb.append("?");
        }

        return sb.toString();
    }
}
