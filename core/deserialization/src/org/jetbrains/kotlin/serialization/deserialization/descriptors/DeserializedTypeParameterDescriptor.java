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

package org.jetbrains.kotlin.serialization.deserialization.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.SourceElement;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.AbstractLazyTypeParameterDescriptor;
import org.jetbrains.kotlin.serialization.ProtoBuf;
import org.jetbrains.kotlin.serialization.deserialization.DeserializationContext;
import org.jetbrains.kotlin.serialization.deserialization.DeserializationPackage;
import org.jetbrains.kotlin.serialization.deserialization.TypeDeserializer;
import org.jetbrains.kotlin.types.JetType;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilPackage.getBuiltIns;

public class DeserializedTypeParameterDescriptor extends AbstractLazyTypeParameterDescriptor {
    private final ProtoBuf.TypeParameter proto;
    private final TypeDeserializer typeDeserializer;

    public DeserializedTypeParameterDescriptor(@NotNull DeserializationContext c, @NotNull ProtoBuf.TypeParameter proto, int index) {
        super(c.getStorageManager(),
              c.getContainingDeclaration(),
              c.getNameResolver().getName(proto.getName()),
              DeserializationPackage.variance(proto.getVariance()),
              proto.getReified(),
              index,
              SourceElement.NO_SOURCE);
        this.proto = proto;
        this.typeDeserializer = c.getTypeDeserializer();
    }

    @NotNull
    @Override
    protected Set<JetType> resolveUpperBounds() {
        if (proto.getUpperBoundCount() == 0) {
            return Collections.singleton(getBuiltIns(this).getDefaultBound());
        }
        Set<JetType> result = new LinkedHashSet<JetType>(proto.getUpperBoundCount());
        for (ProtoBuf.Type upperBound : proto.getUpperBoundList()) {
            result.add(typeDeserializer.type(upperBound, Annotations.EMPTY));
        }
        return result;
    }
}
