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

package org.jetbrains.kotlin.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.types.KotlinType;

public abstract class SerializerExtension {
    @NotNull
    public abstract StringTable getStringTable();

    public boolean shouldUseTypeTable() {
        return false;
    }

    public void serializeClass(@NotNull ClassDescriptor descriptor, @NotNull ProtoBuf.Class.Builder proto) {
    }

    public void serializePackage(@NotNull ProtoBuf.Package.Builder proto) {
    }

    public void serializeConstructor(@NotNull ConstructorDescriptor descriptor, @NotNull ProtoBuf.Constructor.Builder proto) {
    }

    public void serializeFunction(@NotNull FunctionDescriptor descriptor, @NotNull ProtoBuf.Function.Builder proto) {
    }

    public void serializeProperty(@NotNull PropertyDescriptor descriptor, @NotNull ProtoBuf.Property.Builder proto) {
    }

    public void serializeEnumEntry(@NotNull ClassDescriptor descriptor, @NotNull ProtoBuf.EnumEntry.Builder proto) {
    }

    public void serializeValueParameter(@NotNull ValueParameterDescriptor descriptor, @NotNull ProtoBuf.ValueParameter.Builder proto) {
    }

    public void serializeType(@NotNull KotlinType type, @NotNull ProtoBuf.Type.Builder proto) {
    }

    public void serializeTypeParameter(@NotNull TypeParameterDescriptor typeParameter, @NotNull ProtoBuf.TypeParameter.Builder proto) {
    }

    public void serializeErrorType(@NotNull KotlinType type, @NotNull ProtoBuf.Type.Builder builder) {
         throw new IllegalStateException("Cannot serialize error type: " + type);
    }
}
