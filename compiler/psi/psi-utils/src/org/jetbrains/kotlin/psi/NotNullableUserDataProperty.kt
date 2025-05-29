/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import kotlin.reflect.KProperty

class NotNullableUserDataProperty<in R : UserDataHolder, T : Any>(val key: Key<T>, val defaultValue: T) {
    operator fun getValue(thisRef: R, desc: KProperty<*>) = thisRef.getUserData(key) ?: defaultValue

    operator fun setValue(thisRef: R, desc: KProperty<*>, value: T) {
        thisRef.putUserData(key, if (value != defaultValue) value else null)
    }
}