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

import jet.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.storage.LockBasedStorageManager;
import org.jetbrains.jet.storage.NotNullLazyValue;
import org.jetbrains.jet.util.ReenteringLazyValueComputationException;
import org.jetbrains.jet.util.Box;

import java.util.List;

import static org.jetbrains.jet.lang.resolve.BindingContext.DEFERRED_TYPE;

public class DeferredType implements JetType {
    
    public static DeferredType create(BindingTrace trace, NotNullLazyValue<JetType> lazyValue) {
        DeferredType deferredType = new DeferredType(lazyValue);
        trace.record(DEFERRED_TYPE, new Box<DeferredType>(deferredType));
        return deferredType;
    }
    
    public static DeferredType create(BindingTrace trace, Function0<JetType> compute) {
        return create(trace, LockBasedStorageManager.NO_LOCKS.createLazyValue(compute));
    }

    private final NotNullLazyValue<JetType> lazyValue;

    private DeferredType(NotNullLazyValue<JetType> lazyValue) {
        this.lazyValue = lazyValue;
    }

    public boolean isComputed() {
        return lazyValue.isComputed();
    }

    @NotNull
    public JetType getActualType() {
        return lazyValue.invoke();
    }

    @Override
    @NotNull
    public JetScope getMemberScope() {
        return getActualType().getMemberScope();
    }

    @Override
    public boolean isError() {
        return getActualType().isError();
    }

    @Override
    @NotNull
    public TypeConstructor getConstructor() {
        return getActualType().getConstructor();
    }

    @Override
    @NotNull
    public List<TypeProjection> getArguments() {
        return getActualType().getArguments();
    }

    @Override
    public boolean isNullable() {
        return getActualType().isNullable();
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return getActualType().getAnnotations();
    }

    @Override
    public String toString() {
        try {
            if (lazyValue.isComputed()) {
                return getActualType().toString();
            }
            else {
                return "<Not computed yet>";
            }
        }
        catch (ReenteringLazyValueComputationException e) {
            return "<Failed to compute this type>";
        }
    }

    @Override
    public boolean equals(Object obj) {
        return getActualType().equals(obj);
    }

    @Override
    public int hashCode() {
        return getActualType().hashCode();
    }
}
