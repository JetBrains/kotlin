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

@file:JvmName("ReflectJvmMapping")
package kotlin.reflect.jvm

import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.kotlin.reflect.ReflectKotlinClass
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBufUtil
import java.lang.reflect.*
import java.util.*
import kotlin.jvm.internal.Reflection
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.KTypeImpl
import kotlin.reflect.jvm.internal.asKCallableImpl
import kotlin.reflect.jvm.internal.asKPropertyImpl

// Kotlin reflection -> Java reflection

/**
 * Returns a Java [Field] instance corresponding to the backing field of the given property,
 * or `null` if the property has no backing field.
 */
val KProperty<*>.javaField: Field?
    get() = this.asKPropertyImpl()?.javaField

/**
 * Returns a Java [Method] instance corresponding to the getter of the given property,
 * or `null` if the property has no getter, for example in case of a simple private `val` in a class.
 */
val KProperty<*>.javaGetter: Method?
    get() = getter.javaMethod

/**
 * Returns a Java [Method] instance corresponding to the setter of the given mutable property,
 * or `null` if the property has no setter, for example in case of a simple private `var` in a class.
 */
val KMutableProperty<*>.javaSetter: Method?
    get() = setter.javaMethod


/**
 * Returns a Java [Method] instance corresponding to the given Kotlin function,
 * or `null` if this function is a constructor or cannot be represented by a Java [Method].
 */
val KFunction<*>.javaMethod: Method?
    get() = this.asKCallableImpl()?.caller?.member as? Method

/**
 * Returns a Java [Constructor] instance corresponding to the given Kotlin function,
 * or `null` if this function is not a constructor or cannot be represented by a Java [Constructor].
 */
@Suppress("UNCHECKED_CAST") val <T> KFunction<T>.javaConstructor: Constructor<T>?
    get() = this.asKCallableImpl()?.caller?.member as? Constructor<T>


/**
 * Returns a Java [Type] instance corresponding to the given Kotlin type.
 * Note that one Kotlin type may correspond to different JVM types depending on where it appears. For example, [Unit] corresponds to
 * the JVM class [Unit] when it's the type of a parameter, or to `void` when it's the return type of a function.
 */
val KType.javaType: Type
    get() = (this as KTypeImpl).javaType



// Java reflection -> Kotlin reflection

/**
 * Returns a [KProperty] instance corresponding to the given Java [Field] instance,
 * or `null` if this field cannot be represented by a Kotlin property
 * (for example, if it is a synthetic field).
 */
val Field.kotlinProperty: KProperty<*>?
    get() {
        if (isSynthetic) return null

        // TODO: optimize (search by name)

        val kotlinPackage = getKPackage()
        if (kotlinPackage != null) {
            return kotlinPackage.members.filterIsInstance<KProperty<*>>().firstOrNull { it.javaField == this }
        }

        return declaringClass.kotlin.memberProperties.firstOrNull { it.javaField == this }
    }


private fun Member.getKPackage(): KDeclarationContainer? {
    // TODO: support multifile classes
    val header = ReflectKotlinClass.create(declaringClass)?.classHeader
    if (header != null && header.kind == KotlinClassHeader.Kind.FILE_FACADE && header.metadataVersion.isCompatible()) {
        // TODO: avoid reading and parsing metadata twice (here and later in KPackageImpl#descriptor)
        val (nameResolver, proto) = JvmProtoBufUtil.readPackageDataFrom(header.data!!, header.strings!!)
        val moduleName =
                if (proto.hasExtension(JvmProtoBuf.packageModuleName))
                    nameResolver.getString(proto.getExtension(JvmProtoBuf.packageModuleName))
                else JvmAbi.DEFAULT_MODULE_NAME
        return Reflection.getOrCreateKotlinPackage(declaringClass, moduleName)
    }

    return null
}

/**
 * Returns a [KFunction] instance corresponding to the given Java [Method] instance,
 * or `null` if this method cannot be represented by a Kotlin function
 * (for example, if it is a synthetic method).
 */
val Method.kotlinFunction: KFunction<*>?
    get() {
        if (isSynthetic) return null

        if (Modifier.isStatic(modifiers)) {
            val kotlinPackage = getKPackage()
            if (kotlinPackage != null) {
                return kotlinPackage.members.filterIsInstance<KFunction<*>>().firstOrNull { it.javaMethod == this }
            }

            // For static bridge method generated for a @JvmStatic function in the companion object, also try to find the latter
            val companion = declaringClass.kotlin.companionObject
            if (companion != null) {
                companion.functions.firstOrNull {
                    val m = it.javaMethod
                    m != null && m.name == this.name &&
                    Arrays.equals(m.parameterTypes, this.parameterTypes) && m.returnType == this.returnType
                }?.let { return it }
            }
        }

        return declaringClass.kotlin.functions.firstOrNull { it.javaMethod == this }
    }

/**
 * Returns a [KFunction] instance corresponding to the given Java [Constructor] instance,
 * or `null` if this constructor cannot be represented by a Kotlin function
 * (for example, if it is a synthetic constructor).
 */
val <T : Any> Constructor<T>.kotlinFunction: KFunction<T>?
    get() {
        if (isSynthetic) return null

        return declaringClass.kotlin.constructors.firstOrNull { it.javaConstructor == this }
    }
