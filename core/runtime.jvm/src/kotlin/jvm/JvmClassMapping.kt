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
@file:JvmName("JvmClassMappingKt")
@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")

package kotlin.jvm

import kotlin.jvm.internal.ClassBasedDeclarationContainer
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass

/**
 * Returns a Java [Class] instance corresponding to the given [KClass] instance.
 */
val <T : Any> KClass<T>.java: Class<T>
    @JvmName("getJavaClass")
    get() = (this as ClassBasedDeclarationContainer).jClass as Class<T>

/**
 * Returns a Java [Class] instance representing the primitive type corresponding to the given [KClass] if it exists.
 */
val <T : Any> KClass<T>.javaPrimitiveType: Class<T>?
    get() {
        val thisJClass = (this as ClassBasedDeclarationContainer).jClass
        if (thisJClass.isPrimitive) return thisJClass as Class<T>

        return when (thisJClass.canonicalName) {
            "java.lang.Boolean"   -> Boolean::class.java
            "java.lang.Character" -> Char::class.java
            "java.lang.Byte"      -> Byte::class.java
            "java.lang.Short"     -> Short::class.java
            "java.lang.Integer"   -> Int::class.java
            "java.lang.Float"     -> Float::class.java
            "java.lang.Long"      -> Long::class.java
            "java.lang.Double"    -> Double::class.java
            else -> null
        } as Class<T>?
    }

/**
 * Returns a Java [Class] instance corresponding to the given [KClass] instance.
 * In case of primitive types it returns corresponding wrapper classes.
 */
val <T : Any> KClass<T>.javaObjectType: Class<T>
    get() {
        val thisJClass = (this as ClassBasedDeclarationContainer).jClass
        if (!thisJClass.isPrimitive) return thisJClass as Class<T>

        return when (thisJClass.canonicalName) {
            "boolean" -> java.lang.Boolean::class.java
            "char"    -> java.lang.Character::class.java
            "byte"    -> java.lang.Byte::class.java
            "short"   -> java.lang.Short::class.java
            "int"     -> java.lang.Integer::class.java
            "float"   -> java.lang.Float::class.java
            "long"    -> java.lang.Long::class.java
            "double"  -> java.lang.Double::class.java
            else -> thisJClass
        } as Class<T>
    }

/**
 * Returns a [KClass] instance corresponding to the given Java [Class] instance.
 */
val <T : Any> Class<T>.kotlin: KClass<T>
    @JvmName("getKotlinClass")
    get() = Reflection.createKotlinClass(this) as KClass<T>


/**
 * Returns the runtime Java class of this object.
 */
val <T: Any> T.javaClass : Class<T>
    @Suppress("UsePropertyAccessSyntax")
    get() = (this as java.lang.Object).getClass() as Class<T>

@Deprecated("Use 'java' property to get Java class corresponding to this Kotlin class or cast this instance to Any if you really want to get the runtime Java class of this implementation of KClass.", ReplaceWith("(this as Any).javaClass"), level = DeprecationLevel.ERROR)
val <T: Any> KClass<T>.javaClass: Class<KClass<T>>
    @JvmName("getRuntimeClassOfKClassInstance")
    @Suppress("UsePropertyAccessSyntax")
    get() = (this as java.lang.Object).getClass() as Class<KClass<T>>

/**
 * Checks if array can contain element of type [T].
 */
@Suppress("REIFIED_TYPE_PARAMETER_NO_INLINE")
fun <reified T : Any> Array<*>.isArrayOf(): Boolean =
        T::class.java.isAssignableFrom(this.javaClass.componentType)

/**
 * Returns a [KClass] instance corresponding to the annotation type of this annotation.
 */
val <T : Annotation> T.annotationClass: KClass<out T>
    get() = (this as java.lang.annotation.Annotation).annotationType().kotlin as KClass<out T>
