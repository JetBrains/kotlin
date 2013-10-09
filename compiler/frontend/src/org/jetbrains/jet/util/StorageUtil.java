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

package org.jetbrains.jet.util;

import jet.Function0;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.storage.NotNullLazyValue;

import static org.jetbrains.jet.storage.LockBasedStorageManager.NO_LOCKS;

public class StorageUtil {
    private static final Function1 EMPTY_CONSUMER = new Function1<Object, Void>() {
                @Override
                public Void invoke(Object t) {
                    return null;
                }
            };


    public static <T> NotNullLazyValue<T> createRecursionIntolerantLazyValueWithDefault(
            @NotNull final T defaultValue,
            @NotNull Function0<T> compute
    ) {
        //noinspection unchecked
        return NO_LOCKS.createLazyValueWithPostCompute(
                compute,
                new Function1<Boolean, T>() {
                    @Override
                    public T invoke(Boolean firstTime) {
                        if (firstTime) throw new ReenteringLazyValueComputationException();
                        return defaultValue;
                    }
                },
                EMPTY_CONSUMER
        );
    }
}
