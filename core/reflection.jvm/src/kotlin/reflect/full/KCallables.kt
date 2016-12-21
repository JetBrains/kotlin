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
package kotlin.reflect.full

import kotlin.reflect.*

/**
 * Returns a parameter representing the `this` instance needed to call this callable,
 * or `null` if this callable is not a member of a class and thus doesn't take such parameter.
 */
@SinceKotlin("1.1")
val KCallable<*>.instanceParameter: KParameter?
    get() = parameters.singleOrNull { it.kind == KParameter.Kind.INSTANCE }

/**
 * Returns a parameter representing the extension receiver instance needed to call this callable,
 * or `null` if this callable is not an extension.
 */
@SinceKotlin("1.1")
val KCallable<*>.extensionReceiverParameter: KParameter?
    get() = parameters.singleOrNull { it.kind == KParameter.Kind.EXTENSION_RECEIVER }

/**
 * Returns parameters of this callable, excluding the `this` instance and the extension receiver parameter.
 */
@SinceKotlin("1.1")
val KCallable<*>.valueParameters: List<KParameter>
    get() = parameters.filter { it.kind == KParameter.Kind.VALUE }

/**
 * Returns the parameter of this callable with the given name, or `null` if there's no such parameter.
 */
@SinceKotlin("1.1")
fun KCallable<*>.findParameterByName(name: String): KParameter? {
    return parameters.singleOrNull { it.name == name }
}
