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
import kotlin.reflect.jvm.internal.KMemberPropertyImpl
import kotlin.reflect.jvm.internal.KMutableMemberPropertyImpl

public var <R> KProperty<R>.accessible: Boolean
        get() {
            return when (this) {
                is KMutableMemberPropertyImpl<*, R> ->
                        field?.isAccessible() ?: true &&
                        getter?.isAccessible() ?: true &&
                        setter?.isAccessible() ?: true
                is KMemberPropertyImpl<*, R> ->
                        field?.isAccessible() ?: true &&
                        getter?.isAccessible() ?: true
                else -> {
                    // Non-member properties always have public visibility on JVM, thus accessible has no effect on them
                    true
                }
            }
        }
        set(value) {
            when (this) {
                is KMutableMemberPropertyImpl<*, R> -> {
                    field?.setAccessible(value)
                    getter?.setAccessible(value)
                    setter?.setAccessible(value)
                }
                is KMemberPropertyImpl<*, R> -> {
                    field?.setAccessible(value)
                    getter?.setAccessible(value)
                }
            }
        }
