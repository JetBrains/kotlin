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

package kotlinx.cinterop

import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.primaryConstructor

typealias NativePtr = Long
val nativeNullPtr: NativePtr = 0L

// TODO: the functions below should eventually be intrinsified

inline fun <reified T : CVariable> typeOf() = T::class.companionObjectInstance as CVariable.Type

/**
 * Returns interpretation of entity with given pointer, or `null` if it is null.
 *
 * @param T must not be abstract
 */
inline fun <reified T : NativePointed> interpretNullablePointed(ptr: NativePtr): T? {
    if (ptr == nativeNullPtr) {
        return null
    } else {
        val kClass = T::class
        val primaryConstructor = kClass.primaryConstructor
        if (primaryConstructor == null) {
            throw IllegalArgumentException("${kClass.simpleName} doesn't have a constructor")
        }
        @Suppress("UNCHECKED_CAST")
        return (primaryConstructor as (NativePtr) -> T)(ptr)
    }
}

fun <T : CPointed> interpretCPointer(rawValue: NativePtr) =
        if (rawValue == nativeNullPtr) {
            null
        } else {
            CPointer<T>(rawValue)
        }

inline fun <reified T : CAdaptedFunctionType<*>> CAdaptedFunctionType.Companion.getInstanceOf(): T =
        T::class.objectInstance!!

internal fun CPointer<*>.cPointerToString() = "CPointer(raw=0x%x)".format(rawValue)

/**
 * Returns a pointer to `T`-typed C function which calls given Kotlin *static* function.
 * @see CAdaptedFunctionType.fromStatic
 */
inline fun <reified F : Function<*>, reified T : CAdaptedFunctionType<F>> staticCFunction(body: F): CFunctionPointer<T> {
    val type = CAdaptedFunctionType.getInstanceOf<T>()
    return interpretPointed<CFunction<T>>(type.fromStatic(body)).ptr
}

