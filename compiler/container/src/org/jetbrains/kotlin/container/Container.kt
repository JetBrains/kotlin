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

import java.io.Closeable
import java.io.PrintStream
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

class ContainerConsistencyException(message: String) : Exception(message)

public interface ComponentContainer {
    fun createResolveContext(requestingDescriptor: ValueDescriptor): ValueResolveContext
}

public interface ComponentProvider {
    public fun resolve(request: Type): ValueDescriptor?
    public fun <T> create(request: Class<T>): T
}

object DynamicComponentDescriptor : ValueDescriptor {
    override fun getValue(): Any = throw UnsupportedOperationException()
    override fun toString(): String = "Dynamic"
}

public class StorageComponentContainer(id: String) : ComponentContainer, ComponentProvider, Closeable {
    public val unknownContext: ComponentResolveContext by lazy { ComponentResolveContext(this, DynamicComponentDescriptor) }
    val componentStorage = ComponentStorage(id)

    override fun createResolveContext(requestingDescriptor: ValueDescriptor): ValueResolveContext {
        if (requestingDescriptor == DynamicComponentDescriptor) // cache unknown component descriptor
            return unknownContext
        return ComponentResolveContext(this, requestingDescriptor)
    }

    fun compose(): StorageComponentContainer {
        componentStorage.compose(unknownContext)
        return this
    }

    fun dump(printer: PrintStream) {
        componentStorage.dump(printer)
    }

    override fun close() = componentStorage.dispose()

    public fun resolve(request: Type, context: ValueResolveContext): ValueDescriptor? {
        return componentStorage.resolve(request, context) ?: resolveIterable(request, context)
    }

    override fun resolve(request: Type): ValueDescriptor? {
        return resolve(request, unknownContext)
    }

    private fun resolveIterable(request: Type, context: ValueResolveContext): ValueDescriptor? {
        if (request !is ParameterizedType) return null
        val rawType = request.getRawType()
        if (rawType != javaClass<Iterable<*>>()) return null
        val typeArguments = request.getActualTypeArguments()
        if (typeArguments.size() != 1) return null
        val iterableTypeArgument = typeArguments[0]
        val iterableType = when (iterableTypeArgument) {
            is WildcardType -> {
                val upperBounds = iterableTypeArgument.getUpperBounds()
                if (upperBounds.size() != 1) return null
                upperBounds[0]
            }
            is Class<*> -> iterableTypeArgument
            is ParameterizedType -> iterableTypeArgument
            else -> return null
        }
        return IterableDescriptor(componentStorage.resolveMultiple(iterableType, context))
    }

    public fun resolveMultiple(request: Class<*>, context: ValueResolveContext = unknownContext): Iterable<ValueDescriptor> {
        return componentStorage.resolveMultiple(request, context)
    }

    public fun registerDescriptors(descriptors: List<ComponentDescriptor>): StorageComponentContainer {
        componentStorage.registerDescriptors(unknownContext, descriptors)
        return this
    }

    override fun <T> create(request: Class<T>): T {
        val constructorBinding = request.bindToConstructor(unknownContext)
        val args = constructorBinding.argumentDescriptors.map { it.getValue() }.toTypedArray()
        return constructorBinding.constructor.newInstance(*args) as T
    }
}

public fun StorageComponentContainer.registerSingleton(klass: Class<*>): StorageComponentContainer {
    return registerDescriptors(listOf(SingletonTypeComponentDescriptor(this, klass)))
}

public fun StorageComponentContainer.registerInstance(instance: Any): StorageComponentContainer {
    return registerDescriptors(listOf(InstanceComponentDescriptor(instance)))
}

public inline fun <reified T : Any> StorageComponentContainer.resolve(context: ValueResolveContext = unknownContext): ValueDescriptor? {
    return resolve(javaClass<T>(), context)
}

public inline fun <reified T : Any> StorageComponentContainer.resolveMultiple(context: ValueResolveContext = unknownContext): Iterable<ValueDescriptor> {
    return resolveMultiple(javaClass<T>(), context)
}
