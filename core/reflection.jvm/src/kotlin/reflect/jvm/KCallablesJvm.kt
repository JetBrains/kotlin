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

@file:JvmName("KCallablesJvm")
package kotlin.reflect.jvm

import java.lang.reflect.AccessibleObject
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.internal.asKCallableImpl

/**
 * Provides a way to suppress JVM access checks for a callable.
 *
 * @getter returns `true` if JVM access checks are suppressed for this callable object.
 *         For a property, that means that all its accessors (getter, and setter for `var` properties) are accessible.
 *
 * @setter if set to `true`, suppresses JVM access checks for this callable object.
 *         For a property, both accessors are made accessible.
 *
 * @see [java.lang.reflect.AccessibleObject]
 */
var KCallable<*>.isAccessible: Boolean
    get() {
        return when (this) {
            is KMutableProperty ->
                javaField?.isAccessible ?: true &&
                javaGetter?.isAccessible ?: true &&
                javaSetter?.isAccessible ?: true
            is KProperty ->
                javaField?.isAccessible ?: true &&
                javaGetter?.isAccessible ?: true
            is KProperty.Getter ->
                property.javaField?.isAccessible ?: true &&
                javaMethod?.isAccessible ?: true
            is KMutableProperty.Setter<*> ->
                property.javaField?.isAccessible ?: true &&
                javaMethod?.isAccessible ?: true
            is KFunction ->
                javaMethod?.isAccessible ?: true &&
                (this.asKCallableImpl()?.defaultCaller?.member as? AccessibleObject)?.isAccessible ?: true &&
                this.javaConstructor?.isAccessible ?: true
            else -> throw UnsupportedOperationException("Unknown callable: $this ($javaClass)")
        }
    }
    set(value) {
        when (this) {
            is KMutableProperty -> {
                javaField?.isAccessible = value
                javaGetter?.isAccessible = value
                javaSetter?.isAccessible = value
            }
            is KProperty -> {
                javaField?.isAccessible = value
                javaGetter?.isAccessible = value
            }
            is KProperty.Getter -> {
                property.javaField?.isAccessible = value
                javaMethod?.isAccessible = value
            }
            is KMutableProperty.Setter<*> -> {
                property.javaField?.isAccessible = value
                javaMethod?.isAccessible = value
            }
            is KFunction -> {
                javaMethod?.isAccessible = value
                (this.asKCallableImpl()?.defaultCaller?.member as? AccessibleObject)?.isAccessible = true
                this.javaConstructor?.isAccessible = value
            }
            else -> throw UnsupportedOperationException("Unknown callable: $this ($javaClass)")
        }
    }
