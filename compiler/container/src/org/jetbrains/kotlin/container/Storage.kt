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
import java.io.Closeable
import java.io.PrintStream
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.ArrayList
import java.util.HashSet
import java.util.LinkedHashSet

enum class ComponentStorageState {
    Initial,
    Initialized,
    Disposing,
    Disposed
}

internal class InvalidCardinalityException(message: String, val descriptors: Collection<ComponentDescriptor>) : Exception(message)

class ComponentStorage(val myId: String, parent: ComponentStorage?) : ValueResolver {
    var state = ComponentStorageState.Initial
    private val registry = ComponentRegistry()
    init {
        parent?.let { registry.addAll(it.registry) }
    }

    private val descriptors = LinkedHashSet<ComponentDescriptor>()
    private val dependencies = MultiMap.createLinkedSet<ComponentDescriptor, Type>()

    override fun resolve(request: Type, context: ValueResolveContext): ValueDescriptor? {
        if (state == ComponentStorageState.Initial)
            throw ContainerConsistencyException("Container was not composed before resolving")

        val entry = registry.tryGetEntry(request)
        if (entry.isNotEmpty()) {
            registerDependency(request, context)

            if (entry.size > 1)
                throw InvalidCardinalityException("Request $request cannot be satisfied because there is more than one type registered", entry)
            return entry.singleOrNull()
        }
        return null
    }

    private fun registerDependency(request: Type, context: ValueResolveContext) {
        if (context is ComponentResolveContext) {
            val descriptor = context.requestingDescriptor
            if (descriptor is ComponentDescriptor) {
                dependencies.putValue(descriptor, request)
            }
        }
    }

    fun dump(printer: PrintStream): Unit = with (printer) {
        val heading = "Container: $myId"
        println(heading)
        println("=".repeat(heading.length))
        println()
        getDescriptorsInDisposeOrder().forEach { descriptor ->
            println(descriptor)
            dependencies[descriptor].forEach {
                print("   -> ")
                val typeName = it.toString()
                print(typeName.substringBefore(" ")) // interface, class
                print(" ")
                print(typeName.substringAfterLast(".")) // name
                val resolve = registry.tryGetEntry(it)
                print(" as ")
                print(resolve)
                println()
            }
            println()
        }
    }


    fun resolveMultiple(request: Type, context: ValueResolveContext): Iterable<ValueDescriptor> {
        registerDependency(request, context)
        return registry.tryGetEntry(request)
    }

    internal fun registerDescriptors(context: ComponentResolveContext, items: List<ComponentDescriptor>) {
        if (state == ComponentStorageState.Disposed) {
            throw ContainerConsistencyException("Cannot register descriptors in $state state")
        }

        for (descriptor in items)
            descriptors.add(descriptor)

        if (state == ComponentStorageState.Initialized)
            composeDescriptors(context, items)

    }

    fun compose(context: ComponentResolveContext) {
        if (state != ComponentStorageState.Initial)
            throw ContainerConsistencyException("Container $myId was already composed.")

        state = ComponentStorageState.Initialized
        composeDescriptors(context, descriptors)
    }

    private fun composeDescriptors(context: ComponentResolveContext, descriptors: Collection<ComponentDescriptor>) {
        if (descriptors.isEmpty()) return

        registry.addAll(descriptors)

        val implicits = inspectDependenciesAndRegisterAdhoc(context, descriptors)

        injectProperties(context, descriptors + implicits)
    }

    private fun injectProperties(context: ComponentResolveContext, components: Collection<ComponentDescriptor>) {
        for (component in components) {
            if (component.shouldInjectProperties) {
                injectProperties(component.getValue(), context.container.createResolveContext(component))
            }
        }
    }

    private fun inspectDependenciesAndRegisterAdhoc(context: ComponentResolveContext, descriptors: Collection<ComponentDescriptor>): LinkedHashSet<ComponentDescriptor> {
        val adhoc = LinkedHashSet<ComponentDescriptor>()
        val visitedTypes = HashSet<Type>()
        for (descriptor in descriptors) {
            collectAdhocComponents(context, descriptor, visitedTypes, adhoc)
        }
        registry.addAll(adhoc)
        return adhoc
    }

    private fun collectAdhocComponents(context: ComponentResolveContext, descriptor: ComponentDescriptor,
                                       visitedTypes: HashSet<Type>, adhocDescriptors: LinkedHashSet<ComponentDescriptor>
    ) {
        val dependencies = descriptor.getDependencies(context)
        for (type in dependencies) {
            if (!visitedTypes.add(type))
                continue

            val entry = registry.tryGetEntry(type)
            if (entry.isEmpty()) {
                val rawType: Class<*>? = when (type) {
                    is Class<*> -> type
                    is ParameterizedType -> type.rawType as? Class<*>
                    else -> null
                }
                if (rawType == null)
                    continue

                if (!Modifier.isAbstract(rawType.modifiers) && !rawType.isPrimitive) {
                    val implicitDescriptor = ImplicitSingletonTypeComponentDescriptor(context.container, rawType)
                    adhocDescriptors.add(implicitDescriptor)
                    collectAdhocComponents(context, implicitDescriptor, visitedTypes, adhocDescriptors)
                }
            }
        }
    }

    private fun injectProperties(instance: Any, context: ValueResolveContext) {
        val classInfo = instance.javaClass.getInfo()

        classInfo.setterInfos.forEach { (method) ->
            val methodBinding = method.bindToMethod(context)
            methodBinding.invoke(instance)
        }
    }

    fun dispose() {
        if (state != ComponentStorageState.Initialized) {
            if (state == ComponentStorageState.Initial)
                return // it is valid to dispose container which was not initialized
            throw ContainerConsistencyException("Component container cannot be disposed in the $state state.")
        }

        state = ComponentStorageState.Disposing
        val disposeList = getDescriptorsInDisposeOrder()
        for (descriptor in disposeList)
            disposeDescriptor(descriptor)
        state = ComponentStorageState.Disposed
    }

    private fun getDescriptorsInDisposeOrder(): List<ComponentDescriptor> {
        return topologicalSort(descriptors) {
            val dependent = ArrayList<ComponentDescriptor>()
            for (interfaceType in dependencies[it]) {
                for (dependency in registry.tryGetEntry(interfaceType)) {
                    dependent.add(dependency)
                }
            }
            dependent
        }
    }

    private fun disposeDescriptor(descriptor: ComponentDescriptor) {
        if (descriptor is Closeable)
            descriptor.close()
    }
}