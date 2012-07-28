/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen.intrinsics;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Map;

/**
 * @author Stepan Koltsov
 */
class IntrinsicsMap {


    private static final class Key {
        @NotNull
        private final FqNameUnsafe owner;
        @NotNull
        private final Name name;
        private final int valueParameterCount;

        private Key(@NotNull FqNameUnsafe owner, @NotNull Name name, int valueParameterCount) {
            this.owner = owner;
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

            return true;
        }

        @Override
        public int hashCode() {
            int result = owner.hashCode();
            result = 31 * result + name.hashCode();
            result = 31 * result + valueParameterCount;
            return result;
        }
    }

    private int valueParameterCountForKey(@NotNull CallableMemberDescriptor member) {
        if (member instanceof PropertyDescriptor) {
            return -1;
        }
        else {
            return member.getValueParameters().size();
        }
    }

    private Map<Key, IntrinsicMethod> intrinsicsMap = Maps.newHashMap();


    /**
     * @param valueParameterCount -1 for property
     */
    public void registerIntrinsic(@NotNull FqName owner, @NotNull Name name, int valueParameterCount, @NotNull IntrinsicMethod impl) {
        intrinsicsMap.put(new Key(owner.toUnsafe(), name, valueParameterCount), impl);
    }


    @Nullable
    public IntrinsicMethod getIntrinsic(@NotNull CallableMemberDescriptor descriptor) {
        Key key = new Key(
                DescriptorUtils.getFQName(descriptor.getContainingDeclaration()),
                descriptor.getName(),
                valueParameterCountForKey(descriptor));
        return intrinsicsMap.get(key);
    }

}
