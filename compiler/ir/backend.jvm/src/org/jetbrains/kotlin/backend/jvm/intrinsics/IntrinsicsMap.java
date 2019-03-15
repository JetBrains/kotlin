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

package org.jetbrains.kotlin.backend.jvm.intrinsics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.resolve.DescriptorUtils;

import java.util.HashMap;
import java.util.Map;

class IntrinsicsMap {
    private static final class Key {
        private final FqNameUnsafe owner;
        private final FqNameUnsafe receiverParameter;
        private final String name;
        private final int valueParameterCount;

        private Key(@NotNull FqNameUnsafe owner, @Nullable FqNameUnsafe receiverParameter, @NotNull String name, int valueParameterCount) {
            this.owner = owner;
            this.receiverParameter = receiverParameter;
            this.name = name;
            this.valueParameterCount = valueParameterCount;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (valueParameterCount != key.valueParameterCount) return false;
            if (!name.equals(key.name)) return false;
            if (!owner.equals(key.owner)) return false;
            if (receiverParameter == null ? key.receiverParameter != null : !receiverParameter.equals(key.receiverParameter)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = owner.hashCode();
            result = 31 * result + name.hashCode();
            result = 31 * result + valueParameterCount;
            result = 31 * result + (receiverParameter != null ? receiverParameter.hashCode() : 0);
            return result;
        }
    }

    private static int valueParameterCountForKey(@NotNull CallableMemberDescriptor member) {
        if (member instanceof PropertyDescriptor) {
            return -1;
        }
        else {
            return member.getValueParameters().size();
        }
    }

    private final Map<Key, IntrinsicMethod> intrinsicsMap = new HashMap<>();


    /**
     * @param valueParameterCount -1 for property
     */
    public void registerIntrinsic(
            @NotNull FqName owner,
            @Nullable FqNameUnsafe receiverParameter,
            @NotNull String name,
            int valueParameterCount,
            @NotNull IntrinsicMethod impl
    ) {
        intrinsicsMap.put(new Key(owner.toUnsafe(), receiverParameter, name, valueParameterCount), impl);
    }

    @Nullable
    public IntrinsicMethod getIntrinsic(@NotNull CallableMemberDescriptor descriptor) {
        Key key = new Key(
                DescriptorUtils.getFqName(descriptor.getContainingDeclaration()),
                getReceiverParameterFqName(descriptor),
                descriptor.getName().asString(),
                valueParameterCountForKey(descriptor)
        );
        return intrinsicsMap.get(key);
    }

    @Nullable
    private static FqNameUnsafe getReceiverParameterFqName(@NotNull CallableMemberDescriptor descriptor) {
        ReceiverParameterDescriptor receiverParameter = descriptor.getExtensionReceiverParameter();
        if (receiverParameter == null) return null;

        ClassifierDescriptor classifier = receiverParameter.getType().getConstructor().getDeclarationDescriptor();
        if (classifier == null) return null;

        if (classifier instanceof TypeParameterDescriptor) {
            return IntrinsicMethods.RECEIVER_PARAMETER_FQ_NAME;
        }

        return DescriptorUtils.getFqName(classifier);
    }
}
