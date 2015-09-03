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

package org.jetbrains.kotlin.storage

public interface MemoizedFunctionToNotNull<P, R : Any> : Function1<P, R> {
    public fun isComputed(key: P): Boolean
}

public interface MemoizedFunctionToNullable<P, R : Any> : Function1<P, R?> {
    public fun isComputed(key: P): Boolean
}

public interface NotNullLazyValue<T : Any> : Function0<T> {
    public fun isComputed(): Boolean
}

public interface NullableLazyValue<T : Any> : Function0<T?> {
    public fun isComputed(): Boolean
}

public fun <T : Any> NotNullLazyValue<T>.get(_this: Any?, p: PropertyMetadata): T = invoke()
