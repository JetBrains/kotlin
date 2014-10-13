/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.psi

import kotlin.properties.ReadWriteProperty
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder

public class UserDataProperty<in R: UserDataHolder, T : Any>(val key: Key<T>, val default: T? = null) : ReadWriteProperty<R, T?> {
    override fun get(thisRef: R, desc: kotlin.PropertyMetadata): T? {
        return thisRef.getUserData(key)
    }

    override fun set(thisRef: R, desc: kotlin.PropertyMetadata, value: T?) {
        thisRef.putUserData(key, value)
    }
}

public class NotNullableUserDataProperty<in R: UserDataHolder, T : Any>(val key: Key<T>, val defaultValue: T) : ReadWriteProperty<R, T> {
    override fun get(thisRef: R, desc: kotlin.PropertyMetadata): T {
        return thisRef.getUserData(key) ?: defaultValue
    }

    override fun set(thisRef: R, desc: kotlin.PropertyMetadata, value: T) {
        thisRef.putUserData(key, value)
    }
}