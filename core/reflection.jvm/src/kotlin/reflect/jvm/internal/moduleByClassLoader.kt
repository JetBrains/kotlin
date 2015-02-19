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

import org.jetbrains.kotlin.load.java.structure.reflect.classLoader
import org.jetbrains.kotlin.load.kotlin.reflect.RuntimeModuleData
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

// TODO: collect nulls periodically
private val moduleByClassLoader: ConcurrentMap<WeakClassLoaderBox, WeakReference<RuntimeModuleData>> = ConcurrentHashMap()

private class WeakClassLoaderBox(classLoader: ClassLoader) {
    val ref: WeakReference<ClassLoader> = WeakReference(classLoader)

    // Identity hash code is saved because otherwise once the weak reference is GC'd we cannot compute it anymore
    val identityHashCode: Int = System.identityHashCode(classLoader)

    // Temporary strong reference to the class loader to ensure it won't get GC'd when we're inserting this box into the map
    var temporaryStrongRef: ClassLoader? = classLoader

    override fun equals(other: Any?) =
            other is WeakClassLoaderBox && ref.get() === other.ref.get()

    override fun hashCode() =
            identityHashCode
}

private fun Class<*>.getOrCreateModule(): RuntimeModuleData {
    val classLoader = this.classLoader
    val key = WeakClassLoaderBox(classLoader)

    val cached = moduleByClassLoader[key]
    if (cached != null) {
        cached.get()?.let { return it }
        moduleByClassLoader.remove(key, cached)
    }

    val module = RuntimeModuleData.create(classLoader)
    try {
        while (true) {
            val ref = moduleByClassLoader.putIfAbsent(key, WeakReference(module))
            if (ref == null) return module

            val result = ref.get()
            if (result != null) return result
            moduleByClassLoader.remove(key, ref)
        }
    }
    finally {
        key.temporaryStrongRef = null
    }
}
