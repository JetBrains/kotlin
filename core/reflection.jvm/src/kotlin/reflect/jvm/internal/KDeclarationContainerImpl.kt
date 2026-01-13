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

import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.runtime.components.RuntimeModuleData
import org.jetbrains.kotlin.descriptors.runtime.components.tryLoadClass
import org.jetbrains.kotlin.descriptors.runtime.structure.safeClassLoader
import org.jetbrains.kotlin.descriptors.runtime.structure.wrapperByPrimitive
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import kotlin.jvm.internal.ClassBasedDeclarationContainer
import kotlin.metadata.KmConstructor
import kotlin.metadata.KmFunction
import kotlin.metadata.KmProperty
import kotlin.metadata.isVar
import kotlin.metadata.jvm.signature
import kotlin.reflect.KProperty0

internal abstract class KDeclarationContainerImpl : ClassBasedDeclarationContainer {
    abstract inner class Data {
        // This is stored here on a soft reference to prevent GC from destroying the weak reference to it in the moduleByClassLoader cache
        val moduleData: RuntimeModuleData by ReflectProperties.lazySoft {
            jClass.getOrCreateModule()
        }
    }

    protected open val methodOwner: Class<*>
        get() = jClass.wrapperByPrimitive ?: jClass

    abstract val functionsMetadata: Collection<KmFunction>

    abstract val propertiesMetadata: Collection<KmProperty>

    abstract val constructorsMetadata: Collection<KmConstructor>

    abstract val constructorDescriptors: Collection<ConstructorDescriptor>

    abstract fun getProperties(name: Name): Collection<PropertyDescriptor>

    abstract fun getFunctions(name: Name): Collection<FunctionDescriptor>

    abstract fun getLocalPropertyDescriptor(index: Int): PropertyDescriptor?

    abstract fun getLocalPropertyMetadata(index: Int): KmProperty?

    private val classLoader: ClassLoader get() = jClass.safeClassLoader

    fun createLocalProperty(index: Int, signature: String): KProperty0<*>? {
        val kmProperty = getLocalPropertyMetadata(index) ?: return null
        if (kmProperty.receiverParameterType != null) {
            throw KotlinReflectionInternalError("Local property ${kmProperty.name} is an extension, which is not yet supported")
        }

        return if (kmProperty.isVar)
            KotlinKMutableProperty0<Any?>(this, signature, rawBoundReceiver = null, kmProperty, KCallableOverriddenStorage.EMPTY)
        else
            KotlinKProperty0<Any?>(this, signature, rawBoundReceiver = null, kmProperty, KCallableOverriddenStorage.EMPTY)
    }

    fun findPropertyMetadata(name: String, signature: String): KmProperty {
        // For class properties, we'll also need to support the case when there are several properties with the same name,
        // see `findPropertyDescriptor`.
        require(this is KPackageImpl) { "Only top-level properties are supported for now: $this/$name ($signature)" }

        val properties = propertiesMetadata.filter { it.name == name && it.computeJvmSignature(this) == signature }
        if (properties.isEmpty()) {
            throw KotlinReflectionInternalError("Property '$name' (JVM signature: $signature) not resolved in $this")
        }

        if (properties.size > 1) {
            throw KotlinReflectionInternalError("Property '$name' (JVM signature: $signature) resolved in several methods in $this")
        }

        return properties.single()
    }

    fun findPropertyDescriptor(name: String, signature: String): PropertyDescriptor {
        val match = LOCAL_PROPERTY_SIGNATURE.matchEntire(signature)
        if (match != null) {
            val (number) = match.destructured
            return getLocalPropertyDescriptor(number.toInt())
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

    fun findFunctionMetadata(name: String, signature: String): KmFunction {
        require(this is KPackageImpl) { "Only top-level functions are supported for now: $this/$name ($signature)" }

        val functions = functionsMetadata.filter { it.name == name && it.signature.toString() == signature }
        if (functions.size != 1) {
            val allMembers = functionsMetadata.joinToString("\n") { function ->
                function.name + " | " + function.signature
            }
            throw KotlinReflectionInternalError(
                "Function '$name' (JVM signature: $signature) not resolved in $this:" +
                        if (allMembers.isEmpty()) " no members found" else " several matching members found:\n$allMembers"
            )
        }

        return functions.single()
    }

    fun findFunctionDescriptor(name: String, signature: String): FunctionDescriptor {
        val members = if (name == "<init>") constructorDescriptors.toList() else getFunctions(Name.identifier(name))
        val functions = members.filter { descriptor ->
            RuntimeTypeMapper.mapSignature(descriptor).asString() == signature
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

    fun findConstructorMetadata(signature: String): KmConstructor =
        constructorsMetadata.singleOrNull { it.signature.toString() == signature } ?: run {
            val allMembers = constructorsMetadata.joinToString("\n") { constructor -> constructor.signature.toString() }
            throw KotlinReflectionInternalError(
                "Constructor (JVM signature: $signature) not resolved in $this:" +
                        if (allMembers.isEmpty()) " no constructors found" else " several matching constructors found:\n$allMembers"
            )
        }

    fun findJavaConstructor(signature: String): Constructor<*> =
        jClass.declaredConstructors.singleOrNull { it.jvmSignature == signature } ?: run {
            val allMembers = jClass.declaredConstructors.joinToString("\n") { constructor -> constructor.jvmSignature }
            throw KotlinReflectionInternalError(
                "Constructor (JVM signature: $signature) not resolved in $this:" +
                        if (allMembers.isEmpty()) " no constructors found" else "\n$allMembers"
            )
        }

    private fun Class<*>.lookupMethod(
        name: String, parameterTypes: Array<Class<*>>, returnType: Class<*>, isStaticDefault: Boolean,
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

        val functionJvmDescriptor = classLoader.parseAndLoadDescriptor(desc, loadReturnType = true)
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

    fun findDefaultMethod(name: String, desc: String, isMember: Boolean, hasExtensionParameter: Boolean): Method? {
        if (name == "<init>") return null

        val parameterTypes = arrayListOf<Class<*>>()
        if (isMember) {
            // Note that this value is replaced inside the lookupMethod call below, for each class/interface in the hierarchy.
            parameterTypes.add(jClass)
        }
        val jvmDescriptor = classLoader.parseAndLoadDescriptor(desc, loadReturnType = true)
        addParametersAndMasks(parameterTypes, jvmDescriptor.parameters, isConstructor = false, hasExtensionParameter)

        return methodOwner.lookupMethod(
            name + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX, parameterTypes.toTypedArray(), jvmDescriptor.returnType!!, isStaticDefault = isMember
        )
    }

    fun findConstructorBySignature(desc: String): Constructor<*>? =
        jClass.tryGetConstructor(classLoader.parseAndLoadDescriptor(desc, loadReturnType = false).parameters)

    fun findDefaultConstructor(desc: String): Constructor<*>? =
        jClass.tryGetConstructor(arrayListOf<Class<*>>().also { parameterTypes ->
            val parsedParameters = classLoader.parseAndLoadDescriptor(desc, loadReturnType = false).parameters
            addParametersAndMasks(parameterTypes, parsedParameters, isConstructor = true, hasExtensionParameter = false)
        })

    private fun addParametersAndMasks(
        result: MutableList<Class<*>>,
        parameters: List<Class<*>>,
        isConstructor: Boolean,
        hasExtensionParameter: Boolean,
    ) {
        // Constructors that include parameters of inline class types contain an extra trailing DEFAULT_CONSTRUCTOR_MARKER parameter,
        // which should be excluded when calculating mask size.
        val withoutMarker =
            if (parameters.lastOrNull() == DEFAULT_CONSTRUCTOR_MARKER) parameters.subList(0, parameters.size - 1)
            else parameters

        val allocatedBitsForDefaultMask = if (hasExtensionParameter) withoutMarker.size - 1 else withoutMarker.size

        result.addAll(withoutMarker)
        repeat((allocatedBitsForDefaultMask + Integer.SIZE - 1) / Integer.SIZE) {
            result.add(Integer.TYPE)
        }

        result.add(if (isConstructor) DEFAULT_CONSTRUCTOR_MARKER else Any::class.java)
    }

    companion object {
        private val DEFAULT_CONSTRUCTOR_MARKER = Class.forName("kotlin.jvm.internal.DefaultConstructorMarker")

        @JvmField
        val LOCAL_PROPERTY_SIGNATURE = "<v#(\\d+)>".toRegex()
    }
}
