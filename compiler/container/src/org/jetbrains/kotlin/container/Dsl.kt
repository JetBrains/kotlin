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

package org.jetbrains.kotlin.container

import kotlin.reflect.KProperty

fun composeContainer(id: String, parent: StorageComponentContainer? = null, init: StorageComponentContainer.() -> Unit): StorageComponentContainer {
    val c = StorageComponentContainer(id, parent)
    c.init()
    c.compose()
    return c
}

inline fun <reified T : Any> StorageComponentContainer.useImpl() {
    registerSingleton(T::class.java)
}

inline fun <reified T : Any> ComponentProvider.get(): T {
    return getService(T::class.java)
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> ComponentProvider.tryGetService(request: Class<T>): T? {
    return resolve(request)?.getValue() as T?
}

fun <T : Any> ComponentProvider.getService(request: Class<T>): T {
    return tryGetService(request) ?: throw UnresolvedServiceException(this, request)
}

fun StorageComponentContainer.useInstance(instance: Any) {
    registerInstance(instance)
}

fun StorageComponentContainer.useClashResolver(clashResolver: PlatformExtensionsClashResolver<*>) {
    registerClashResolvers(listOf(clashResolver))
}

inline operator fun <reified T : Any> ComponentProvider.getValue(thisRef: Any?, desc: KProperty<*>): T {
    return getService(T::class.java)
}

class UnresolvedServiceException(container: ComponentProvider, request: Class<*>) :
    IllegalArgumentException("Unresolved service: $request in $container")