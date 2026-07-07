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

package org.jetbrains.kotlin.psi

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import kotlin.reflect.KProperty

/**
 * A property delegate that reads and writes a value stored in a [UserDataHolder] under the given [key].
 *
 * It lets user data — the platform's mechanism for attaching arbitrary values to a [UserDataHolder] such as a PSI
 * element — be accessed as an ordinary, type-safe (nullable) Kotlin property.
 *
 * ### Example:
 *
 * ```kotlin
 * private val KEY = Key.create<Boolean>("MY_FLAG")
 * var PsiElement.myFlag: Boolean? by UserDataProperty(KEY)
 * ```
 *
 * @param R the type of user-data holder this property applies to
 * @param T the type of the stored value
 */
class UserDataProperty<in R : UserDataHolder, T : Any>(val key: Key<T>) {
    /** Reads the value stored under [key] on [thisRef], or `null` if none is set. */
    operator fun getValue(thisRef: R, desc: KProperty<*>) = thisRef.getUserData(key)

    /** Stores [value] under [key] on [thisRef] (or removes it when [value] is `null`). */
    operator fun setValue(thisRef: R, desc: KProperty<*>, value: T?) = thisRef.putUserData(key, value)
}