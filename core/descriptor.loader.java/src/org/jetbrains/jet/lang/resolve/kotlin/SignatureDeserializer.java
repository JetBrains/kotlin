/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.kotlin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.descriptors.serialization.JavaProtoBuf;
import org.jetbrains.jet.descriptors.serialization.NameResolver;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

public class SignatureDeserializer {
    // These types are ordered according to their sorts, this is significant for deserialization
    private static final char[] PRIMITIVE_TYPES = new char[] { 'V', 'Z', 'C', 'B', 'S', 'I', 'F', 'J', 'D' };

    private final NameResolver nameResolver;

    public SignatureDeserializer(@NotNull NameResolver nameResolver) {
        this.nameResolver = nameResolver;
    }

    @NotNull
    public String methodSignatureString(@NotNull JavaProtoBuf.JavaMethodSignature signature) {
        Name name = nameResolver.getName(signature.getName());

        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (int i = 0, length = signature.getParameterTypeCount(); i < length; i++) {
            typeDescriptor(signature.getParameterType(i), sb);
        }
        sb.append(')');
        typeDescriptor(signature.getReturnType(), sb);

        return name.asString() + sb.toString();
    }

    @NotNull
    public DescriptorLoadersStorage.MemberSignature methodSignature(@NotNull JavaProtoBuf.JavaMethodSignature signature) {
        return DescriptorLoadersStorage.MemberSignature.fromMethodNameAndDesc(methodSignatureString(signature));
    }

    @NotNull
    public String typeDescriptor(@NotNull JavaProtoBuf.JavaType type) {
        return typeDescriptor(type, new StringBuilder()).toString();
    }

    @NotNull
    private StringBuilder typeDescriptor(@NotNull JavaProtoBuf.JavaType type, @NotNull StringBuilder sb) {
        for (int i = 0; i < type.getArrayDimension(); i++) {
            sb.append('[');
        }

        if (type.hasPrimitiveType()) {
            sb.append(PRIMITIVE_TYPES[type.getPrimitiveType().ordinal()]);
        }
        else {
            sb.append("L");
            sb.append(fqNameToInternalName(nameResolver.getFqName(type.getClassFqName())));
            sb.append(";");
        }

        return sb;
    }

    @NotNull
    private static String fqNameToInternalName(@NotNull FqName fqName) {
        return fqName.asString().replace('.', '/');
    }
}
