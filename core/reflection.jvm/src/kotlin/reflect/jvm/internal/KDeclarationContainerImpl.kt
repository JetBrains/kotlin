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
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.structure.reflect.createArrayType
import org.jetbrains.kotlin.load.java.structure.reflect.safeClassLoader
import org.jetbrains.kotlin.load.kotlin.reflect.RuntimeModuleData
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import kotlin.jvm.internal.ClassBasedDeclarationContainer
import kotlin.reflect.KotlinReflectionInternalError

internal abstract class KDeclarationContainerImpl : ClassBasedDeclarationContainer {
    // Note: this is stored here on a soft reference to prevent GC from destroying the weak reference to it in the moduleByClassLoader cache
    private val moduleData_ = ReflectProperties.lazySoft {
        jClass.getOrCreateModule()
    }

    val moduleData: RuntimeModuleData
        get() = moduleData_()

    abstract val constructorDescriptors: Collection<ConstructorDescriptor>

    abstract fun getProperties(name: Name): Collection<PropertyDescriptor>

    abstract fun getFunctions(name: Name): Collection<FunctionDescriptor>

    fun getMembers(scope: MemberScope, declaredOnly: Boolean, nonExtensions: Boolean, extensions: Boolean): Sequence<KCallableImpl<*>> {
        val visitor = object : DeclarationDescriptorVisitorEmptyBodies<KCallableImpl<*>?, Unit>() {
            private fun skipCallable(descriptor: CallableMemberDescriptor): Boolean {
                if (declaredOnly && !descriptor.kind.isReal) return true

                val isExtension = descriptor.extensionReceiverParameter != null
                if (isExtension && !extensions) return true
                if (!isExtension && !nonExtensions) return true

                return false
            }

            override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: Unit): KCallableImpl<*>? {
                return if (skipCallable(descriptor)) null else createProperty(descriptor)
            }

            override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: Unit): KCallableImpl<*>? {
                return if (skipCallable(descriptor)) null else KFunctionImpl(this@KDeclarationContainerImpl, descriptor)
            }

            override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, data: Unit): KCallableImpl<*>? {
                throw IllegalStateException("No constructors should appear in this scope: $descriptor")
            }
        }

        return scope.getContributedDescriptors().asSequence()
                .filter { descriptor ->
                    descriptor !is MemberDescriptor || descriptor.visibility != Visibilities.INVISIBLE_FAKE
                }
                .mapNotNull { descriptor ->
                    descriptor.accept(visitor, Unit)
                }
    }

    private fun createProperty(descriptor: PropertyDescriptor): KPropertyImpl<*> {
        val receiverCount = (descriptor.dispatchReceiverParameter?.let { 1 } ?: 0) +
                            (descriptor.extensionReceiverParameter?.let { 1 } ?: 0)

        when {
            descriptor.isVar -> when (receiverCount) {
                0 -> return KMutableProperty0Impl<Any?>(this, descriptor)
                1 -> return KMutableProperty1Impl<Any?, Any?>(this, descriptor)
                2 -> return KMutableProperty2Impl<Any?, Any?, Any?>(this, descriptor)
            }
            else -> when (receiverCount) {
                0 -> return KProperty0Impl<Any?>(this, descriptor)
                1 -> return KProperty1Impl<Any?, Any?>(this, descriptor)
                2 -> return KProperty2Impl<Any?, Any?, Any?>(this, descriptor)
            }
        }

        throw KotlinReflectionInternalError("Unsupported property: $descriptor")
    }

    fun findPropertyDescriptor(name: String, signature: String): PropertyDescriptor {
        val properties = getProperties(Name.identifier(name))
                .filter { descriptor ->
                    descriptor is PropertyDescriptor &&
                    RuntimeTypeMapper.mapPropertySignature(descriptor).asString() == signature
                }

        if (properties.size != 1) {
            val debugText = "'$name' (JVM signature: $signature)"
            throw KotlinReflectionInternalError(
                    if (properties.isEmpty()) "Property $debugText not resolved in $this"
                    else "${properties.size} properties $debugText resolved in $this: $properties"
            )
        }

        return properties.single()
    }

    fun findFunctionDescriptor(name: String, signature: String): FunctionDescriptor {
        val functions = (if (name == "<init>") constructorDescriptors.toList() else getFunctions(Name.identifier(name)))
                .filter { descriptor ->
                    RuntimeTypeMapper.mapSignature(descriptor).asString() == signature
                }

        if (functions.size != 1) {
            val debugText = "'$name' (JVM signature: $signature)"
            throw KotlinReflectionInternalError(
                    if (functions.isEmpty()) "Function $debugText not resolved in $this"
                    else "${functions.size} functions $debugText resolved in $this: $functions"
            )
        }

        return functions.single()
    }

    private fun Class<*>.tryGetMethod(name: String, parameterTypes: List<Class<*>>, declared: Boolean) =
            try {
                if (declared) getDeclaredMethod(name, *parameterTypes.toTypedArray())
                else getMethod(name, *parameterTypes.toTypedArray())
            }
            catch (e: NoSuchMethodException) {
                null
            }

    private fun Class<*>.tryGetConstructor(parameterTypes: List<Class<*>>, declared: Boolean) =
            try {
                if (declared) getDeclaredConstructor(*parameterTypes.toTypedArray())
                else getConstructor(*parameterTypes.toTypedArray())
            }
            catch (e: NoSuchMethodException) {
                null
            }

    // TODO: check resulting method's return type
    fun findMethodBySignature(name: String, desc: String, declared: Boolean): Method? {
        if (name == "<init>") return null

        // Method for a top level function should be the one from the package facade.
        // This is likely to change after the package part reform.
        return jClass.tryGetMethod(name, loadParameterTypes(desc), declared)
    }

    fun findDefaultMethod(name: String, desc: String, isMember: Boolean, declared: Boolean): Method? {
        if (name == "<init>") return null

        val parameterTypes = arrayListOf<Class<*>>()
        if (isMember) {
            parameterTypes.add(jClass)
        }
        addParametersAndMasks(parameterTypes, desc, false)

        return jClass.tryGetMethod(name + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX, parameterTypes, declared)
    }

    fun findConstructorBySignature(desc: String, declared: Boolean): Constructor<*>? {
        return jClass.tryGetConstructor(loadParameterTypes(desc), declared)
    }

    fun findDefaultConstructor(desc: String, declared: Boolean): Constructor<*>? {
        val parameterTypes = arrayListOf<Class<*>>()
        addParametersAndMasks(parameterTypes, desc, true)

        return jClass.tryGetConstructor(parameterTypes, declared)
    }

    private fun addParametersAndMasks(result: MutableList<Class<*>>, desc: String, isConstructor: Boolean) {
        val valueParameters = loadParameterTypes(desc)
        result.addAll(valueParameters)
        repeat((valueParameters.size + Integer.SIZE - 1) / Integer.SIZE) {
            result.add(Integer.TYPE)
        }
        result.add(if (isConstructor) DEFAULT_CONSTRUCTOR_MARKER else Any::class.java)
    }

    private fun loadParameterTypes(desc: String): List<Class<*>> {
        val classLoader = jClass.safeClassLoader
        val result = arrayListOf<Class<*>>()

        var i = 1
        while (desc[i] != ')') {
            var arrayDimension = 0
            while (desc[i] == '[') {
                arrayDimension++
                i++
            }

            var type = when (desc[i++]) {
                'L' -> {
                    val semicolon = desc.indexOf(';', i)
                    val internalName = desc.substring(i, semicolon)
                    i = semicolon + 1
                    classLoader.loadClass(internalName.replace('/', '.'))
                }
                'V' -> Void.TYPE
                'Z' -> java.lang.Boolean.TYPE
                'C' -> java.lang.Character.TYPE
                'B' -> java.lang.Byte.TYPE
                'S' -> java.lang.Short.TYPE
                'I' -> java.lang.Integer.TYPE
                'F' -> java.lang.Float.TYPE
                'J' -> java.lang.Long.TYPE
                'D' -> java.lang.Double.TYPE
                else -> throw KotlinReflectionInternalError("Unknown type prefix in the method signature: $desc")
            }

            repeat(arrayDimension) {
                type = type.createArrayType()
            }

            result.add(type)
        }

        return result
    }

    companion object {
        private val DEFAULT_CONSTRUCTOR_MARKER = Class.forName("kotlin.jvm.internal.DefaultConstructorMarker")
    }
}
