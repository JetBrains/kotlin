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

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.load.java.structure.reflect.classId
import org.jetbrains.kotlin.load.java.structure.reflect.classLoader
import org.jetbrains.kotlin.load.java.structure.reflect.createArrayType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf.JvmType.PrimitiveType.*
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.KDeclarationContainer
import kotlin.reflect.KotlinReflectionInternalError

abstract class KCallableContainerImpl : KDeclarationContainer {
    // Note: this is stored here on a soft reference to prevent GC from destroying the weak reference to it in the moduleByClassLoader cache
    val moduleData by ReflectProperties.lazySoft {
        jClass.getOrCreateModule()
    }

    abstract val jClass: Class<*>

    abstract val scope: JetScope

    fun findPropertyDescriptor(name: String, signature: String): PropertyDescriptor {
        val properties = scope
                .getProperties(Name.guess(name))
                .filter { descriptor ->
                    descriptor is PropertyDescriptor &&
                    RuntimeTypeMapper.mapPropertySignature(descriptor) == signature
                }

        if (properties.size() != 1) {
            val debugText = "'$name' (JVM signature: $signature)"
            throw KotlinReflectionInternalError(
                    if (properties.isEmpty()) "Property $debugText not resolved in $this"
                    else "${properties.size()} properties $debugText resolved in $this: $properties"
            )
        }

        return properties.single() as PropertyDescriptor
    }

    fun findFunctionDescriptor(name: String, signature: String): FunctionDescriptor {
        val functions = scope
                .getFunctions(Name.guess(name))
                .filter { descriptor ->
                    RuntimeTypeMapper.mapSignature(descriptor) == signature
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

    // TODO: check resulting method's return type
    fun findMethodBySignature(signature: JvmProtoBuf.JvmMethodSignature, nameResolver: NameResolver, declared: Boolean): Method? {
        val name = nameResolver.getString(signature.getName())
        val classLoader = jClass.classLoader
        val parameterTypes = signature.getParameterTypeList().map { jvmType ->
            loadJvmType(jvmType, nameResolver, classLoader)
        }.toTypedArray()

        return try {
            if (declared) jClass.getDeclaredMethod(name, *parameterTypes)
            else jClass.getMethod(name, *parameterTypes)
        }
        catch (e: NoSuchMethodException) {
            null
        }
    }

    // TODO: check resulting field's type
    fun findFieldBySignature(
            proto: ProtoBuf.Callable,
            signature: JvmProtoBuf.JvmFieldSignature,
            nameResolver: NameResolver
    ): Field? {
        val name = nameResolver.getString(signature.getName())

        val owner =
                when {
                    proto.hasExtension(JvmProtoBuf.implClassName) -> {
                        val implClassName = nameResolver.getName(proto.getExtension(JvmProtoBuf.implClassName))
                        // TODO: store fq name of impl class name in jvm_descriptors.proto
                        val classId = ClassId(jClass.classId.getPackageFqName(), implClassName)
                        jClass.classLoader.loadClass(classId.asSingleFqName().asString())
                    }
                    signature.getIsStaticInOuter() -> {
                        jClass.getEnclosingClass() ?: throw KotlinReflectionInternalError("Inconsistent metadata for field $name in $jClass")
                    }
                    else -> jClass
                }

        return try {
            owner.getDeclaredField(name)
        }
        catch (e: NoSuchFieldException) {
            null
        }
    }

    private fun loadJvmType(
            type: JvmProtoBuf.JvmType,
            nameResolver: NameResolver,
            classLoader: ClassLoader,
            arrayDimension: Int = type.getArrayDimension()
    ): Class<*> {
        if (arrayDimension > 0) {
            return loadJvmType(type, nameResolver, classLoader, arrayDimension - 1).createArrayType()
        }

        if (type.hasPrimitiveType()) {
            return PRIMITIVE_TYPES[type.getPrimitiveType()]
                   ?: throw KotlinReflectionInternalError("Unknown primitive type: ${type.getPrimitiveType()}")
        }

        if (type.hasClassFqName()) {
            val fqName = nameResolver.getFqName(type.getClassFqName())
            return classLoader.loadClass(fqName.asString())
        }

        throw KotlinReflectionInternalError("Inconsistent metadata for JVM type")
    }

    companion object {
        private val PRIMITIVE_TYPES = mapOf(
                VOID to Void.TYPE,
                BOOLEAN to java.lang.Boolean.TYPE,
                CHAR to java.lang.Character.TYPE,
                BYTE to java.lang.Byte.TYPE,
                SHORT to java.lang.Short.TYPE,
                INT to java.lang.Integer.TYPE,
                FLOAT to java.lang.Float.TYPE,
                LONG to java.lang.Long.TYPE,
                DOUBLE to java.lang.Double.TYPE
        )
    }
}
