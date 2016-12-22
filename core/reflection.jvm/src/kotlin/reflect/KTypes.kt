/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

@file:JvmName("KTypes")
package kotlin.reflect

import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.full.withNullability

/**
 * Returns a new type with the same classifier, arguments and annotations as the given type, and with the given nullability.
 */
@SinceKotlin("1.1")
@Deprecated("Use 'withNullability' from kotlin.reflect.full package", ReplaceWith("this.withNullability(nullable)", "kotlin.reflect.full.withNullability"), level = DeprecationLevel.ERROR)
inline fun KType.withNullability(nullable: Boolean): KType = this.withNullability(nullable)


/**
 * Returns `true` if `this` type is the same or is a subtype of [other], `false` otherwise.
 */
@SinceKotlin("1.1")
@Deprecated("Use 'isSubtypeOf' from kotlin.reflect.full package", ReplaceWith("this.isSubtypeOf(other)", "kotlin.reflect.full.isSubtypeOf"), level = DeprecationLevel.ERROR)
inline fun KType.isSubtypeOf(other: KType): Boolean = this.isSubtypeOf(other)

/**
 * Returns `true` if `this` type is the same or is a supertype of [other], `false` otherwise.
 */
@SinceKotlin("1.1")
@Deprecated("Use 'isSupertypeOf' from kotlin.reflect.full package", ReplaceWith("this.isSupertypeOf(other)", "kotlin.reflect.full.isSupertypeOf"), level = DeprecationLevel.ERROR)
inline fun KType.isSupertypeOf(other: KType): Boolean = this.isSupertypeOf(other)
