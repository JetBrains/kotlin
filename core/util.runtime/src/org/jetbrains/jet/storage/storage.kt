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

package org.jetbrains.jet.storage

public trait MemoizedFunctionToNotNull<P, R: Any> : Function1<P, R>
public trait MemoizedFunctionToNullable<P, R: Any> : Function1<P, R?>

public trait NotNullLazyValue<T: Any> : Function0<T> {
    fun isComputed(): Boolean
}

public trait NullableLazyValue<T: Any> : Function0<T?> {
    fun isComputed(): Boolean
}

fun <T> NotNullLazyValue<T>.get(_this: Any?, p: PropertyMetadata): T = invoke()
