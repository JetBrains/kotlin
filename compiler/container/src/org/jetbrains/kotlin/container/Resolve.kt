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

import java.lang.reflect.Constructor
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Type
import java.util.*

interface ValueResolver {
    fun resolve(request: Type, context: ValueResolveContext): ValueDescriptor?
}

interface ValueResolveContext {
    fun resolve(registration: Type): ValueDescriptor?
}

class ComponentResolveContext(
    val container: StorageComponentContainer,
    val requestingDescriptor: ValueDescriptor,
    val parentContext: ValueResolveContext? = null
) : ValueResolveContext {
    override fun resolve(registration: Type): ValueDescriptor? =
        container.resolve(registration, this) ?: parentContext?.resolve(registration)

    override fun toString(): String = "for $requestingDescriptor in $container"
}

class ConstructorBinding(val constructor: Constructor<*>, val argumentDescriptors: List<ValueDescriptor>)

class MethodBinding(val method: Method, private val argumentDescriptors: List<ValueDescriptor>) {
    fun invoke(instance: Any) {
        val arguments = computeArguments(argumentDescriptors).toTypedArray()
        runWithUnwrappingInvocationException { method.invoke(instance, *arguments) }
    }
}

fun computeArguments(argumentDescriptors: List<ValueDescriptor>): List<Any> = argumentDescriptors.map { it.getValue() }

fun Class<*>.bindToConstructor(containerId: String, context: ValueResolveContext): ConstructorBinding {
    val constructorInfo = getInfo().constructorInfo ?: error("No constructor for $this: ${getInfo()} in $containerId")
    val candidate = constructorInfo.constructor
    return ConstructorBinding(candidate, candidate.bindArguments(containerId, constructorInfo.parameters, context))
}

fun Method.bindToMethod(containerId: String, context: ValueResolveContext): MethodBinding {
    return MethodBinding(this, bindArguments(containerId, genericParameterTypes.toList(), context))
}

private fun Member.bindArguments(
    containerId: String,
    parameters: List<Type>,
    context: ValueResolveContext
): List<ValueDescriptor> {
    val bound = ArrayList<ValueDescriptor>(parameters.size)
    var unsatisfied: MutableList<Type>? = null

    for (parameter in parameters) {
        val descriptor = context.resolve(parameter)
        if (descriptor == null) {
            if (unsatisfied == null)
                unsatisfied = ArrayList<Type>()
            unsatisfied.add(parameter)
        } else {
            bound.add(descriptor)
        }
    }
    if (unsatisfied != null) {
        throw UnresolvedDependenciesException("$containerId: Dependencies for `$this` cannot be satisfied:\n  $unsatisfied")
    }
    return bound
}

class UnresolvedDependenciesException(message: String) : Exception(message)
