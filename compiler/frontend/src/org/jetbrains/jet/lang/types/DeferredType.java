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
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.Annotations;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.storage.NotNullLazyValue;
import org.jetbrains.jet.storage.StorageManager;
import org.jetbrains.jet.util.Box;
import org.jetbrains.jet.util.ReenteringLazyValueComputationException;

import java.util.List;

import static org.jetbrains.jet.lang.resolve.BindingContext.DEFERRED_TYPE;

public class DeferredType implements LazyType {

    private static final Function1 EMPTY_CONSUMER = new Function1<Object, Void>() {
        @Override
        public Void invoke(Object t) {
            return null;
        }
    };

    private static final Function1<Boolean,JetType> RECURSION_PREVENTER = new Function1<Boolean, JetType>() {
        @Override
        public JetType invoke(Boolean firstTime) {
            if (firstTime) throw new ReenteringLazyValueComputationException();
            return ErrorUtils.createErrorType("Recursive dependency");
        }
    };

    @NotNull
    public static DeferredType create(
            @NotNull StorageManager storageManager,
            @NotNull BindingTrace trace,
            @NotNull Function0<JetType> compute
    ) {
        DeferredType deferredType = new DeferredType(storageManager.createLazyValue(compute));
        trace.record(DEFERRED_TYPE, new Box<DeferredType>(deferredType));
        return deferredType;
    }
    
    @NotNull
    public static DeferredType createRecursionIntolerant(
            @NotNull StorageManager storageManager,
            @NotNull BindingTrace trace,
            @NotNull Function0<JetType> compute
    ) {
        //noinspection unchecked
        DeferredType deferredType = new DeferredType(storageManager.createLazyValueWithPostCompute(
                compute,
                RECURSION_PREVENTER,
                EMPTY_CONSUMER
        ));
        trace.record(DEFERRED_TYPE, new Box<DeferredType>(deferredType));
        return deferredType;
    }

    private final NotNullLazyValue<JetType> lazyValue;

    private DeferredType(@NotNull NotNullLazyValue<JetType> lazyValue) {
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

    @NotNull
    @Override
    public Annotations getAnnotations() {
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
