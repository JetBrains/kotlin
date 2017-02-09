/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.storage.StorageManager

open class WrappedTypeFactory(private val storageManager: StorageManager) {
    open fun createLazyWrappedType(computation: () -> KotlinType): KotlinType = LazyWrappedType(storageManager, computation)
    open fun createDeferredType(trace: BindingTrace, computation: () -> KotlinType): KotlinType = DeferredType.create(storageManager, trace, computation)
    open fun createRecursionIntolerantDeferredType(trace: BindingTrace, computation: () -> KotlinType): KotlinType = DeferredType.createRecursionIntolerant(storageManager, trace, computation)
}