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

package org.jetbrains.kotlin.util.slicedMap;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class BasicWritableSlice<K, V> extends AbstractWritableSlice<K, V> {

    public static Void initSliceDebugNames(Class<?> declarationOwner) {
        for (Field field : declarationOwner.getFields()) {
            if (!Modifier.isStatic(field.getModifiers())) continue;
            try {
                Object value = field.get(null);
                if (value instanceof BasicWritableSlice) {
                    BasicWritableSlice slice = (BasicWritableSlice) value;
                    slice.debugName = field.getName();
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        return null;
    }
    
    private String debugName;
    private final RewritePolicy rewritePolicy;
    private final boolean isCollective;

    public BasicWritableSlice(RewritePolicy rewritePolicy) {
        this(rewritePolicy, false);
    }

    public BasicWritableSlice(RewritePolicy rewritePolicy, boolean isCollective) {
        super("<BasicWritableSlice>");

        this.rewritePolicy = rewritePolicy;
        this.isCollective = isCollective;
    }

    // True to put, false to skip
    @Override
    public boolean check(K key, V value) {
//        assert key != null : this + " called with null key";
        assert value != null : this + " called with null value";
        return true;
    }

    @Override
    public void afterPut(MutableSlicedMap map, K key, V value) {
        // Do nothing
    }

    @Override
    public V computeValue(SlicedMap map, K key, V value, boolean valueNotFound) {
        if (valueNotFound) assert value == null;
        return value;
    }

    @Override
    public RewritePolicy getRewritePolicy() {
        return rewritePolicy;
    }

    @Override
    public boolean isCollective() {
        return isCollective;
    }

    public void setDebugName(@NotNull String debugName) {
        if (this.debugName != null) {
            throw new IllegalStateException("Debug name already set for " + this);
        }
        this.debugName = debugName;
    }

    @Override
    public String toString() {
        return debugName;
    }

    @Override
    public ReadOnlySlice<K, V> makeRawValueVersion() {
        return new DelegatingSlice<K, V>(this) {
            @Override
            public V computeValue(SlicedMap map, K key, V value, boolean valueNotFound) {
                if (valueNotFound) assert value == null;
                return value;
            }
        };
    }


}
