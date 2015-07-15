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

package kotlin.reflect.jvm

import kotlin.reflect.KProperty
import kotlin.reflect.jvm.internal.KMutablePropertyImpl
import kotlin.reflect.jvm.internal.KPropertyImpl

/**
 * Provides a way to suppress JVM access checks for a property.
 *
 * @getter returns `true` if JVM access checks are suppressed for this property object.
 *         In case of a `var` property, that means that both getter and setter are accessible.
 *
 * @setter if set to `true`, suppresses JVM access checks for this property object.
 *         In case of a `var` property, both getter and setter are made accessible.
 *
 * @see [java.lang.reflect.AccessibleObject]
 */
public var <R> KProperty<R>.accessible: Boolean
        get() {
            return when (this) {
                is KMutablePropertyImpl<R> ->
                        javaField?.isAccessible() ?: true &&
                        javaGetter?.isAccessible() ?: true &&
                        javaSetter?.isAccessible() ?: true
                is KPropertyImpl<R> ->
                        javaField?.isAccessible() ?: true &&
                        javaGetter?.isAccessible() ?: true
                else -> throw UnsupportedOperationException("Unknown property: $this ($javaClass)")
            }
        }
        set(value) {
            when (this) {
                is KMutablePropertyImpl<R> -> {
                    javaField?.setAccessible(value)
                    javaGetter?.setAccessible(value)
                    javaSetter?.setAccessible(value)
                }
                is KPropertyImpl<R> -> {
                    javaField?.setAccessible(value)
                    javaGetter?.setAccessible(value)
                }
                else -> throw UnsupportedOperationException("Unknown property: $this ($javaClass)")
            }
        }
