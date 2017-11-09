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

import java.lang.reflect.*

open class InstanceComponentDescriptor(val instance: Any) : ComponentDescriptor {

    override fun getValue(): Any = instance
    override fun getRegistrations(): Iterable<Type> = instance::class.java.getInfo().registrations

    override fun getDependencies(context: ValueResolveContext): Collection<Class<*>> = emptyList()

    override fun toString(): String {
        return "Instance: ${instance::class.java.simpleName}"
    }
}

class DefaultInstanceComponentDescriptor(instance: Any): InstanceComponentDescriptor(instance) {
    override fun toString() = "Default instance: ${instance.javaClass.simpleName}"
}