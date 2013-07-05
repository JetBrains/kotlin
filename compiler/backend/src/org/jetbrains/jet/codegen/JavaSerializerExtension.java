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

package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.descriptors.serialization.JavaProtoBufUtil;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.descriptors.serialization.SerializerExtension;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;

public class JavaSerializerExtension extends SerializerExtension {
    private final JetTypeMapper typeMapper;

    public JavaSerializerExtension(@NotNull JetTypeMapper typeMapper) {
        this.typeMapper = typeMapper;
    }

    @Override
    public void serializeCallable(@NotNull CallableMemberDescriptor callable, @NotNull ProtoBuf.Callable.Builder proto) {
        JvmMethodSignature signature = mapSignature(callable);
        if (signature != null) {
            JavaProtoBufUtil.saveJavaSignature(proto, signature.getAsmMethod().toString());
        }
    }

    // TODO: this should be done upon generation of the member instead, because we don't know enough at this point to map correctly
    @Nullable
    private JvmMethodSignature mapSignature(@NotNull CallableMemberDescriptor descriptor) {
        if (descriptor instanceof FunctionDescriptor) {
            return typeMapper.mapSignature((FunctionDescriptor) descriptor);
        }
        return null;
    }
}
