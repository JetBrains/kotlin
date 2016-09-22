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

import com.intellij.util.containers.MultiMap
import java.lang.reflect.Type

internal class ComponentRegistry {
    fun buildRegistrationMap(descriptors: Collection<ComponentDescriptor>): MultiMap<Type, ComponentDescriptor> {
        val registrationMap = MultiMap<Type, ComponentDescriptor>()
        for (descriptor in descriptors) {
            for (registration in descriptor.getRegistrations()) {
                registrationMap.putValue(registration, descriptor)
            }
        }
        return registrationMap
    }

    private var registrationMap = MultiMap.createLinkedSet<Type, ComponentDescriptor>()

    fun addAll(descriptors: Collection<ComponentDescriptor>) {
        registrationMap.putAllValues(buildRegistrationMap(descriptors))
    }

    fun tryGetEntry(request: Type): Collection<ComponentDescriptor> {
        return registrationMap.get(request)
    }

    fun addAll(other: ComponentRegistry) {
        registrationMap.putAllValues(other.registrationMap)
    }
}