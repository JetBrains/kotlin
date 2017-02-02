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

@file:JvmName("KCallables")
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
package kotlin.reflect

import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.findParameterByName
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.valueParameters

/**
 * Returns a parameter representing the `this` instance needed to call this callable,
 * or `null` if this callable is not a member of a class and thus doesn't take such parameter.
 */
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'instanceParameter' from kotlin.reflect.full package", ReplaceWith("this.instanceParameter", "kotlin.reflect.full.instanceParameter"), level = DeprecationLevel.ERROR)
@SinceKotlin("1.1")
inline val KCallable<*>.instanceParameter: KParameter?
    get() = this.instanceParameter

/**
 * Returns a parameter representing the extension receiver instance needed to call this callable,
 * or `null` if this callable is not an extension.
 */
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'extensionReceiverParameter' from kotlin.reflect.full package", ReplaceWith("this.extensionReceiverParameter", "kotlin.reflect.full.extensionReceiverParameter"), level = DeprecationLevel.ERROR)
@SinceKotlin("1.1")
inline val KCallable<*>.extensionReceiverParameter: KParameter?
    get() = this.extensionReceiverParameter

/**
 * Returns parameters of this callable, excluding the `this` instance and the extension receiver parameter.
 */
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'valueParameters' from kotlin.reflect.full package", ReplaceWith("this.valueParameters", "kotlin.reflect.full.valueParameters"), level = DeprecationLevel.ERROR)
@SinceKotlin("1.1")
inline val KCallable<*>.valueParameters: List<KParameter>
    get() = this.valueParameters

/**
 * Returns the parameter of this callable with the given name, or `null` if there's no such parameter.
 */
@kotlin.internal.LowPriorityInOverloadResolution
@Deprecated("Use 'findParameterByName' from kotlin.reflect.full package", ReplaceWith("this.findParameterByName", "kotlin.reflect.full.findParameterByName"), level = DeprecationLevel.ERROR)
@SinceKotlin("1.1")
inline fun KCallable<*>.findParameterByName(name: String): KParameter? {
    return this.findParameterByName(name)
}
