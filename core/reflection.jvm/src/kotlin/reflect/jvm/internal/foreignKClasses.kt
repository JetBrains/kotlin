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

package kotlin.reflect.jvm.internal

import java.lang.ref.WeakReference
import kotlin.reflect.jvm.internal.pcollections.HashPMap

// TODO: collect nulls periodically
// Key of the map is Class.getName(), each value is either a WeakReference<KClassImpl<*>> or an Array<WeakReference<KClassImpl<*>>>.
// Arrays are needed because the same class can be loaded by different class loaders, which results in different Class instances.
// This variable is not volatile intentionally: we don't care if there's a data race on it and some KClass instances will be lost.
// We do care however about general performance on read access to it, thus no synchronization is done here whatsoever
private var FOREIGN_K_CLASSES = HashPMap.empty<String, Any>()

// This function is invoked on each reflection access to Java classes, properties, etc. Performance is critical here.
fun <T> foreignKotlinClass(jClass: Class<T>): KClassImpl<T> {
    val name = jClass.getName()
    val cached = FOREIGN_K_CLASSES[name]
    if (cached is WeakReference<*>) {
        val kClass = cached.get() as KClassImpl<T>?
        if (kClass?.jClass == jClass) {
            return kClass!!
        }
    }
    else if (cached != null) {
        // If the cached value is not a weak reference, it's an array of weak references
        cached as Array<WeakReference<KClassImpl<T>>>
        for (ref in cached) {
            val kClass = ref.get()
            if (kClass?.jClass == jClass) {
                return kClass
            }
        }

        // This is the most unlikely case: we found a cached array of references of length at least 2 (can't be 1 because
        // the single element would be cached instead), and none of those classes is the one we're looking for
        val size = cached.size()
        // Don't use Array constructor because it creates a lambda
        val newArray = arrayOfNulls<WeakReference<KClassImpl<*>>>(size + 1)
        // Don't use Arrays.copyOf because it works reflectively
        System.arraycopy(cached, 0, newArray, 0, size)
        val newKClass = KClassImpl(jClass)
        newArray[size] = WeakReference(newKClass)
        FOREIGN_K_CLASSES = FOREIGN_K_CLASSES.plus(name, newArray)
        return newKClass
    }

    val newKClass = KClassImpl(jClass)
    FOREIGN_K_CLASSES = FOREIGN_K_CLASSES.plus(name, WeakReference(newKClass))
    return newKClass
}
