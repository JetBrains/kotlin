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
import org.jetbrains.kotlin.load.java.structure.reflect.classId
import org.jetbrains.kotlin.load.java.structure.reflect.createArrayType
import org.jetbrains.kotlin.load.java.structure.reflect.safeClassLoader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.jvm.internal.ClassBasedDeclarationContainer
import kotlin.reflect.KCallable
import kotlin.reflect.KotlinReflectionInternalError

internal abstract class KDeclarationContainerImpl : ClassBasedDeclarationContainer {
    // Note: this is stored here on a soft reference to prevent GC from destroying the weak reference to it in the moduleByClassLoader cache
    val moduleData by ReflectProperties.lazySoft {
        jClass.getOrCreateModule()
    }

    abstract val constructorDescriptors: Collection<ConstructorDescriptor>

    abstract fun getProperties(name: Name): Collection<PropertyDescriptor>

    abstract fun getFunctions(name: Name): Collection<FunctionDescriptor>

    fun getMembers(scope: JetScope, declaredOnly: Boolean, nonExtensions: Boolean, extensions: Boolean): Sequence<KCallable<*>> {
        val visitor = object : DeclarationDescriptorVisitorEmptyBodies<KCallable<*>?, Unit>() {
            private fun skipCallable(descriptor: CallableMemberDescriptor): Boolean {
                if (declaredOnly && !descriptor.getKind().isReal()) return true

                val isExtension = descriptor.getExtensionReceiverParameter() != null
                if (isExtension && !extensions) return true
                if (!isExtension && !nonExtensions) return true

                return false
            }

            override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: Unit): KCallable<*>? {
                return if (skipCallable(descriptor)) null else createProperty(descriptor)
            }

            override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: Unit): KCallable<*>? {
                return if (skipCallable(descriptor)) null else KFunctionImpl(this@KDeclarationContainerImpl, descriptor)
            }

            override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, data: Unit): KCallable<*>? {
                throw IllegalStateException("No constructors should appear in this scope: $descriptor")
            }
        }

        return scope.getAllDescriptors().asSequence()
                .filter { descriptor ->
                    descriptor !is MemberDescriptor || descriptor.getVisibility() != Visibilities.INVISIBLE_FAKE
                }
                .map { descriptor ->
                    descriptor.accept(visitor, Unit)
                }
                .filterNotNull()
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
        val properties = getProperties(Name.guess(name))
                .filter { descriptor ->
                    descriptor is PropertyDescriptor &&
                    RuntimeTypeMapper.mapPropertySignature(descriptor).asString() == signature
                }

        if (properties.size() != 1) {
            val debugText = "'$name' (JVM signature: $signature)"
            throw KotlinReflectionInternalError(
                    if (properties.isEmpty()) "Property $debugText not resolved in $this"
                    else "${properties.size()} properties $debugText resolved in $this: $properties"
            )
        }

        return properties.single()
    }

    fun findFunctionDescriptor(name: String, signature: String): FunctionDescriptor {
        val functions = (if (name == "<init>") constructorDescriptors.toList() else getFunctions(Name.guess(name)))
                .filter { descriptor ->
                    RuntimeTypeMapper.mapSignature(descriptor).asString() == signature
                }

        if (functions.size() != 1) {
            val debugText = "'$name' (JVM signature: $signature)"
            throw KotlinReflectionInternalError(
                    if (functions.isEmpty()) "Function $debugText not resolved in $this"
                    else "${functions.size()} functions $debugText resolved in $this: $functions"
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
    fun findMethodBySignature(
            @Suppress("UNUSED_PARAMETER") proto: ProtoBuf.Callable,
            signature: JvmProtoBuf.JvmMethodSignature,
            nameResolver: NameResolver,
            declared: Boolean
    ): Method? {
        val name = nameResolver.getString(signature.name)
        if (name == "<init>") return null

        val parameterTypes = loadParameterTypes(nameResolver, signature)

        // Method for a top level function should be the one from the package facade.
        // This is likely to change after the package part reform.
        val owner = jClass

        return owner.tryGetMethod(name, parameterTypes, declared)
    }

    fun findDefaultMethod(
            signature: JvmProtoBuf.JvmMethodSignature,
            nameResolver: NameResolver,
            isMember: Boolean,
            declared: Boolean
    ): Method? {
        val name = nameResolver.getString(signature.name)
        if (name == "<init>") return null

        val parameterTypes = arrayListOf<Class<*>>()
        if (isMember) {
            parameterTypes.add(jClass)
        }
        addParametersAndMasks(parameterTypes, nameResolver, signature)

        return jClass.tryGetMethod(name + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX, parameterTypes, declared)
    }

    fun findConstructorBySignature(
            signature: JvmProtoBuf.JvmMethodSignature,
            nameResolver: NameResolver,
            declared: Boolean
    ): Constructor<*>? {
        if (nameResolver.getString(signature.name) != "<init>") return null

        return jClass.tryGetConstructor(loadParameterTypes(nameResolver, signature), declared)
    }

    fun findDefaultConstructor(
            signature: JvmProtoBuf.JvmMethodSignature,
            nameResolver: NameResolver,
            declared: Boolean
    ): Constructor<*>? {
        if (nameResolver.getString(signature.name) != "<init>") return null

        val parameterTypes = arrayListOf<Class<*>>()
        addParametersAndMasks(parameterTypes, nameResolver, signature)
        parameterTypes.add(DEFAULT_CONSTRUCTOR_MARKER)

        return jClass.tryGetConstructor(parameterTypes, declared)
    }

    private fun addParametersAndMasks(
            result: MutableList<Class<*>>, nameResolver: NameResolver, signature: JvmProtoBuf.JvmMethodSignature
    ) {
        val valueParameters = loadParameterTypes(nameResolver, signature)
        result.addAll(valueParameters)
        repeat((valueParameters.size() + Integer.SIZE - 1) / Integer.SIZE) {
            result.add(Integer.TYPE)
        }
    }

    private fun loadParameterTypes(nameResolver: NameResolver, signature: JvmProtoBuf.JvmMethodSignature): List<Class<*>> {
        val classLoader = jClass.safeClassLoader
        val result = arrayListOf<Class<*>>()
        val desc = nameResolver.getString(signature.desc)

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

    // TODO: check resulting field's type
    fun findFieldBySignature(
            proto: ProtoBuf.Callable,
            signature: JvmProtoBuf.JvmFieldSignature,
            nameResolver: NameResolver
    ): Field? {
        val name = nameResolver.getString(signature.getName())

        val owner =
                implClassForCallable(nameResolver, proto) ?:
                if (signature.getIsStaticInOuter()) {
                    jClass.getEnclosingClass() ?: throw KotlinReflectionInternalError("Inconsistent metadata for field $name in $jClass")
                }
                else jClass

        return try {
            owner.getDeclaredField(name)
        }
        catch (e: NoSuchFieldException) {
            null
        }
    }

    // Returns the JVM class which contains this callable. This class may be different from the one represented by descriptors
    // in case of top level functions (their bodies are in package parts), methods with implementations in interfaces, etc.
    private fun implClassForCallable(nameResolver: NameResolver, proto: ProtoBuf.Callable): Class<*>? {
        if (!proto.hasExtension(JvmProtoBuf.implClassName)) return null

        val implClassName = nameResolver.getName(proto.getExtension(JvmProtoBuf.implClassName))
        // TODO: store fq name of impl class name in jvm_descriptors.proto
        val classId = ClassId(jClass.classId.getPackageFqName(), implClassName)
        return jClass.safeClassLoader.loadClass(classId.asSingleFqName().asString())
    }

    companion object {
        private val DEFAULT_CONSTRUCTOR_MARKER = Class.forName("kotlin.jvm.internal.DefaultConstructorMarker")
    }
}
