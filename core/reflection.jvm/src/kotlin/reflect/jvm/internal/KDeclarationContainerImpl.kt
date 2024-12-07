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

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.runtime.components.RuntimeModuleData
import org.jetbrains.kotlin.descriptors.runtime.components.tryLoadClass
import org.jetbrains.kotlin.descriptors.runtime.structure.safeClassLoader
import org.jetbrains.kotlin.descriptors.runtime.structure.wrapperByPrimitive
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.isMultiFieldValueClass
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import kotlin.jvm.internal.ClassBasedDeclarationContainer
import kotlin.reflect.jvm.internal.calls.toJvmDescriptor

internal abstract class KDeclarationContainerImpl : ClassBasedDeclarationContainer {
    abstract inner class Data {
        // This is stored here on a soft reference to prevent GC from destroying the weak reference to it in the moduleByClassLoader cache
        val moduleData: RuntimeModuleData by ReflectProperties.lazySoft {
            jClass.getOrCreateModule()
        }
    }

    protected open val methodOwner: Class<*>
        get() = jClass.wrapperByPrimitive ?: jClass

    abstract val constructorDescriptors: Collection<ConstructorDescriptor>

    abstract fun getProperties(name: Name): Collection<PropertyDescriptor>

    abstract fun getFunctions(name: Name): Collection<FunctionDescriptor>

    abstract fun getLocalProperty(index: Int): PropertyDescriptor?

    protected fun getMembers(scope: MemberScope, belonginess: MemberBelonginess): Collection<KCallableImpl<*>> {
        val visitor = object : CreateKCallableVisitor(this) {
            override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, data: Unit): KCallableImpl<*> =
                throw IllegalStateException("No constructors should appear here: $descriptor")
        }
        return scope.getContributedDescriptors().mapNotNull { descriptor ->
            if (descriptor is CallableMemberDescriptor &&
                descriptor.visibility != DescriptorVisibilities.INVISIBLE_FAKE &&
                belonginess.accept(descriptor)
            ) descriptor.accept(visitor, Unit) else null
        }.toList()
    }

    protected enum class MemberBelonginess {
        DECLARED,
        INHERITED;

        fun accept(member: CallableMemberDescriptor): Boolean =
            member.kind.isReal == (this == DECLARED)
    }

    fun findPropertyDescriptor(name: String, signature: String): PropertyDescriptor {
        val match = LOCAL_PROPERTY_SIGNATURE.matchEntire(signature)
        if (match != null) {
            val (number) = match.destructured
            return getLocalProperty(number.toInt())
                ?: throw KotlinReflectionInternalError("Local property #$number not found in $jClass")
        }

        val properties = getProperties(Name.identifier(name))
            .filter { descriptor ->
                RuntimeTypeMapper.mapPropertySignature(descriptor).asString() == signature
            }

        if (properties.isEmpty()) {
            throw KotlinReflectionInternalError("Property '$name' (JVM signature: $signature) not resolved in $this")
        }

        if (properties.size != 1) {
            // Try working around the case of a Java class with a field 'foo' and a method 'getFoo' which overrides Kotlin property 'foo'.
            // Such class has two property descriptors with the name 'foo' in its scope and they may be indistinguishable from each other.
            // However, it's not possible to write 'A::foo' if they're indistinguishable; overload resolution would not be able to choose
            // between the two. So we assume that one of the properties must have a greater visibility than the other, and try loading
            // that one first.
            // Note that this heuristic may result in _incorrect behavior_ if a KProperty object for a less visible property is obtained
            // by other means (through reflection API) and then the soft-referenced descriptor instance for that property is invalidated
            // because there's no more memory left. In that case the KProperty object will now point to another (more visible) property.
            // TODO: consider writing additional info (besides signature) to property reference objects to distinguish them in this case

            val mostVisibleProperties = properties
                .groupBy { it.visibility }
                .toSortedMap { first, second ->
                    DescriptorVisibilities.compare(first, second) ?: 0
                }.values.last()
            if (mostVisibleProperties.size == 1) {
                return mostVisibleProperties.first()
            }

            val allMembers = getProperties(Name.identifier(name)).joinToString("\n") { descriptor ->
                DescriptorRenderer.DEBUG_TEXT.render(descriptor) + " | " + RuntimeTypeMapper.mapPropertySignature(descriptor).asString()
            }
            throw KotlinReflectionInternalError(
                "Property '$name' (JVM signature: $signature) not resolved in $this:" +
                        if (allMembers.isEmpty()) " no members found" else "\n$allMembers"
            )
        }

        return properties.single()
    }

    fun findFunctionDescriptor(name: String, signature: String): FunctionDescriptor {
        val members: Collection<FunctionDescriptor>
        val functions: List<FunctionDescriptor>
        if (name == "<init>") {
            members = constructorDescriptors.toList()
            functions = members.filter { descriptor ->
                val descriptorSignature = if (descriptor.isPrimary && descriptor.containingDeclaration.isMultiFieldValueClass()) {
                    val initial = RuntimeTypeMapper.mapSignature(descriptor).asString()
                    require(initial.startsWith("constructor-impl") && initial.endsWith(")V")) {
                        "Invalid signature of $descriptor: $initial"
                    }
                    initial.removeSuffix("V") + descriptor.containingDeclaration.toJvmDescriptor()
                } else {
                    RuntimeTypeMapper.mapSignature(descriptor).asString()
                }
                descriptorSignature == signature
            }
        } else {
            members = getFunctions(Name.identifier(name))
            functions = members.filter { descriptor -> RuntimeTypeMapper.mapSignature(descriptor).asString() == signature }
        }

        if (functions.size != 1) {
            val allMembers = members.joinToString("\n") { descriptor ->
                DescriptorRenderer.DEBUG_TEXT.render(descriptor) + " | " + RuntimeTypeMapper.mapSignature(descriptor).asString()
            }
            throw KotlinReflectionInternalError(
                "Function '$name' (JVM signature: $signature) not resolved in $this:" +
                        if (allMembers.isEmpty()) " no members found" else "\n$allMembers"
            )
        }

        return functions.single()
    }

    private fun Class<*>.lookupMethod(
        name: String, parameterTypes: Array<Class<*>>, returnType: Class<*>, isStaticDefault: Boolean
    ): Method? {
        // Static "$default" method in any class takes an instance of that class as the first parameter.
        if (isStaticDefault) {
            parameterTypes[0] = this
        }

        tryGetMethod(name, parameterTypes, returnType)?.let { return it }

        superclass?.lookupMethod(name, parameterTypes, returnType, isStaticDefault)?.let { return it }

        // TODO: avoid exponential complexity here
        for (superInterface in interfaces) {
            superInterface.lookupMethod(name, parameterTypes, returnType, isStaticDefault)?.let { return it }

            // Static "$default" methods should be looked up in each DefaultImpls class, see KT-33430
            if (isStaticDefault) {
                val defaultImpls = superInterface.safeClassLoader.tryLoadClass(superInterface.name + JvmAbi.DEFAULT_IMPLS_SUFFIX)
                if (defaultImpls != null) {
                    parameterTypes[0] = superInterface
                    defaultImpls.tryGetMethod(name, parameterTypes, returnType)?.let { return it }
                }
            }
        }

        return null
    }

    private fun Class<*>.tryGetMethod(name: String, parameterTypes: Array<Class<*>>, returnType: Class<*>): Method? =
        try {
            val result = getDeclaredMethod(name, *parameterTypes)

            if (result.returnType == returnType) result
            else {
                // If we've found a method with an unexpected return type, it's likely that there are several methods in this class
                // with the given parameter types and Java reflection API has returned not the one we're looking for.
                // Falling back to enumerating all methods in the class in this (rather rare) case.
                // Example: class A(val x: Int) { fun getX(): String = ... }
                declaredMethods.firstOrNull { method ->
                    method.name == name && method.returnType == returnType && method.parameterTypes.contentEquals(parameterTypes)
                }
            }
        } catch (e: NoSuchMethodException) {
            null
        }

    private fun Class<*>.tryGetConstructor(parameterTypes: List<Class<*>>): Constructor<*>? =
        try {
            getDeclaredConstructor(*parameterTypes.toTypedArray())
        } catch (e: NoSuchMethodException) {
            null
        }

    fun findMethodBySignature(name: String, desc: String): Method? {
        if (name == "<init>") return null

        val functionJvmDescriptor = parseJvmDescriptor(desc, parseReturnType = true)
        val parameterTypes = functionJvmDescriptor.parameters.toTypedArray()
        val returnType = functionJvmDescriptor.returnType!!
        methodOwner.lookupMethod(name, parameterTypes, returnType, false)?.let { return it }

        // Methods from java.lang.Object (equals, hashCode, toString) cannot be found in the interface via
        // Class.getMethod/getDeclaredMethod, so for interfaces, we also look in java.lang.Object.
        if (methodOwner.isInterface) {
            Any::class.java.lookupMethod(name, parameterTypes, returnType, false)?.let { return it }
        }

        return null
    }

    fun findDefaultMethod(name: String, desc: String, isMember: Boolean): Method? {
        if (name == "<init>") return null

        val parameterTypes = arrayListOf<Class<*>>()
        if (isMember) {
            // Note that this value is replaced inside the lookupMethod call below, for each class/interface in the hierarchy.
            parameterTypes.add(jClass)
        }
        val jvmDescriptor = parseJvmDescriptor(desc, parseReturnType = true)
        addParametersAndMasks(parameterTypes, jvmDescriptor.parameters, isConstructor = false)

        return methodOwner.lookupMethod(
            name + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX, parameterTypes.toTypedArray(), jvmDescriptor.returnType!!, isStaticDefault = isMember
        )
    }

    fun findConstructorBySignature(desc: String): Constructor<*>? =
        jClass.tryGetConstructor(parseJvmDescriptor(desc, parseReturnType = false).parameters)

    fun findDefaultConstructor(desc: String): Constructor<*>? =
        jClass.tryGetConstructor(arrayListOf<Class<*>>().also { parameterTypes ->
            val parsedParameters = parseJvmDescriptor(desc, parseReturnType = false).parameters
            addParametersAndMasks(parameterTypes, parsedParameters, isConstructor = true)
        })

    private fun addParametersAndMasks(result: MutableList<Class<*>>, valueParameters: List<Class<*>>, isConstructor: Boolean) {
        // Constructors that include parameters of inline class types contain an extra trailing DEFAULT_CONSTRUCTOR_MARKER parameter,
        // which should be excluded when calculating mask size.
        val withoutMarker =
            if (valueParameters.lastOrNull() == DEFAULT_CONSTRUCTOR_MARKER) valueParameters.subList(0, valueParameters.size - 1)
            else valueParameters

        result.addAll(withoutMarker)
        repeat((withoutMarker.size + Integer.SIZE - 1) / Integer.SIZE) {
            result.add(Integer.TYPE)
        }

        result.add(if (isConstructor) DEFAULT_CONSTRUCTOR_MARKER else Any::class.java)
    }

    private class FunctionJvmDescriptor(val parameters: List<Class<*>>, val returnType: Class<*>?)

    private fun parseJvmDescriptor(desc: String, parseReturnType: Boolean): FunctionJvmDescriptor {
        val result = arrayListOf<Class<*>>()

        var begin = 1
        while (desc[begin] != ')') {
            var end = begin
            while (desc[end] == '[') end++
            @Suppress("SpellCheckingInspection")
            when (desc[end]) {
                in "VZCBSIFJD" -> end++
                'L' -> end = desc.indexOf(';', begin) + 1
                else -> throw KotlinReflectionInternalError("Unknown type prefix in the method signature: $desc")
            }

            result.add(parseType(desc, begin, end))
            begin = end
        }

        val returnType = if (parseReturnType) parseType(desc, begin = begin + 1, end = desc.length) else null

        return FunctionJvmDescriptor(result, returnType)
    }

    private fun parseType(desc: String, begin: Int, end: Int): Class<*> =
        when (desc[begin]) {
            'L' -> jClass.safeClassLoader.loadClass(desc.substring(begin + 1, end - 1).replace('/', '.'))
            '[' -> parseType(desc, begin + 1, end).createArrayType()
            'V' -> Void.TYPE
            'Z' -> Boolean::class.java
            'C' -> Char::class.java
            'B' -> Byte::class.java
            'S' -> Short::class.java
            'I' -> Int::class.java
            'F' -> Float::class.java
            'J' -> Long::class.java
            'D' -> Double::class.java
            else -> throw KotlinReflectionInternalError("Unknown type prefix in the method signature: $desc")
        }

    companion object {
        private val DEFAULT_CONSTRUCTOR_MARKER = Class.forName("kotlin.jvm.internal.DefaultConstructorMarker")

        internal val LOCAL_PROPERTY_SIGNATURE = "<v#(\\d+)>".toRegex()
    }
}
