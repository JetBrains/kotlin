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

package org.jetbrains.kotlin.types;

import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.storage.NotNullLazyValue;
import org.jetbrains.kotlin.storage.StorageManager;
import org.jetbrains.kotlin.util.Box;
import org.jetbrains.kotlin.util.ReenteringLazyValueComputationException;

import static org.jetbrains.kotlin.resolve.BindingContext.DEFERRED_TYPE;

public class DeferredType extends WrappedType {

    private static final Function1 EMPTY_CONSUMER = new Function1<Object, Void>() {
        @Override
        public Void invoke(Object t) {
            return null;
        }
    };

    private static final Function1<Boolean,KotlinType> RECURSION_PREVENTER = new Function1<Boolean, KotlinType>() {
        @Override
        public KotlinType invoke(Boolean firstTime) {
            if (firstTime) throw new ReenteringLazyValueComputationException();
            return ErrorUtils.createErrorType("Recursive dependency");
        }
    };

    @NotNull
    public static DeferredType create(
            @NotNull StorageManager storageManager,
            @NotNull BindingTrace trace,
            @NotNull Function0<KotlinType> compute
    ) {
        DeferredType deferredType = new DeferredType(storageManager.createLazyValue(compute));
        trace.record(DEFERRED_TYPE, new Box<DeferredType>(deferredType));
        return deferredType;
    }
    
    @NotNull
    public static DeferredType createRecursionIntolerant(
            @NotNull StorageManager storageManager,
            @NotNull BindingTrace trace,
            @NotNull Function0<KotlinType> compute
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

    private final NotNullLazyValue<KotlinType> lazyValue;

    private DeferredType(@NotNull NotNullLazyValue<KotlinType> lazyValue) {
        this.lazyValue = lazyValue;
    }

    public boolean isComputing() {
        return lazyValue.isComputing();
    }

    @Override
    public boolean isComputed() {
        return lazyValue.isComputed();
    }

    @NotNull
    @Override
    public KotlinType getDelegate() {
        return lazyValue.invoke();
    }

    @NotNull
    @Override
    public String toString() {
        try {
            if (lazyValue.isComputed()) {
                return getDelegate().toString();
            }
            else {
                return "<Not computed yet>";
            }
        }
        catch (ReenteringLazyValueComputationException e) {
            return "<Failed to compute this type>";
        }
    }
}
