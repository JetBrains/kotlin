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

package org.jetbrains.jet.descriptors.serialization.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.descriptors.serialization.TypeDeserializer;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.SourceElement;
import org.jetbrains.jet.lang.descriptors.impl.AbstractLazyTypeParameterDescriptor;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.storage.StorageManager;

import java.util.LinkedHashSet;
import java.util.Set;

public class DeserializedTypeParameterDescriptor extends AbstractLazyTypeParameterDescriptor {

    private final ProtoBuf.TypeParameter proto;
    private final TypeDeserializer typeDeserializer;

    public DeserializedTypeParameterDescriptor(
            @NotNull StorageManager storageManager,
            @NotNull TypeDeserializer typeDeserializer,
            @NotNull ProtoBuf.TypeParameter proto,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Name name,
            @NotNull Variance variance,
            boolean isReified,
            int index
    ) {
        super(storageManager, containingDeclaration, name, variance, isReified, index, SourceElement.NO_SOURCE);
        this.proto = proto;
        this.typeDeserializer = typeDeserializer;
    }

    @NotNull
    @Override
    protected Set<JetType> resolveUpperBounds() {
        Set<JetType> result = new LinkedHashSet<JetType>(proto.getUpperBoundCount());
        for (ProtoBuf.Type upperBound : proto.getUpperBoundList()) {
            result.add(typeDeserializer.type(upperBound));
        }
        if (result.isEmpty()) {
            result.add(KotlinBuiltIns.getInstance().getDefaultBound());
        }
        return result;
    }

    public int getProtoId() {
        return proto.getId();
    }
}
