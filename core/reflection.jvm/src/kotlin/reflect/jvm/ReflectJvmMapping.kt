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

import org.jetbrains.kotlin.descriptors.runtime.components.ReflectKotlinClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import java.lang.reflect.*
import kotlin.reflect.*
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.internal.*
import kotlin.reflect.javaType as stdlibJavaType

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
@Suppress("UNCHECKED_CAST")
val <T> KFunction<T>.javaConstructor: Constructor<T>?
    get() = this.asKCallableImpl()?.caller?.member as? Constructor<T>


/**
 * Returns a Java [Type] instance corresponding to the given Kotlin type.
 * Note that one Kotlin type may correspond to different JVM types depending on where it appears. For example, [Unit] corresponds to
 * the JVM class [Unit] when it's the type of a parameter, or to `void` when it's the return type of a function.
 */
val KType.javaType: Type
    @OptIn(ExperimentalStdlibApi::class)
    get() = stdlibJavaType


// Java reflection -> Kotlin reflection

/**
 * Returns a [KProperty] instance corresponding to the given Java [Field] instance,
 * or `null` if this field cannot be represented by a Kotlin property
 * (for example, if it is a synthetic field).
 */
val Field.kotlinProperty: KProperty<*>?
    get() {
        if (isSynthetic) return null

        if (Modifier.isStatic(modifiers)) {
            val kotlinPackage = getKPackage()
            if (kotlinPackage != null) {
                return kotlinPackage.members.findKProperty(this)
            }

            val companionKClass = declaringClass.kotlin.companionObject
            if (companionKClass != null) {
                val companionField = declaringClass.getDeclaredFieldOrNull(name)
                if (companionField != null) {
                    companionKClass.memberProperties.findKProperty(companionField)?.let { return it }
                }
            }
        }

        return declaringClass.kotlin.memberProperties.findKProperty(this)
    }


private fun Member.getKPackage(): KDeclarationContainer? =
    when (ReflectKotlinClass.create(declaringClass)?.classHeader?.kind) {
        KotlinClassHeader.Kind.FILE_FACADE, KotlinClassHeader.Kind.MULTIFILE_CLASS, KotlinClassHeader.Kind.MULTIFILE_CLASS_PART ->
            KPackageImpl(declaringClass)
        else -> null
    }

/**
 * Returns a [KFunction] instance corresponding to the given Java [Method] instance,
 * or `null` if this method cannot be represented by a Kotlin function.
 */
val Method.kotlinFunction: KFunction<*>?
    get() {
        if (Modifier.isStatic(modifiers)) {
            val kotlinPackage = getKPackage()
            if (kotlinPackage != null) {
                return kotlinPackage.members.findKFunction(this)
            }

            // For static bridge method generated for a @JvmStatic function in the companion object, also try to find the latter
            val companionKClass = declaringClass.kotlin.companionObject
            if (companionKClass != null) {
                val companionMethod = companionKClass.java.getDeclaredMethodOrNull(name, *parameterTypes)
                if (companionMethod != null) {
                    companionKClass.functions.findKFunction(companionMethod)?.let { return it }
                }
            }
        }

        return declaringClass.kotlin.functions.findKFunction(this)
    }

private fun Collection<KCallable<*>>.findKFunction(method: Method): KFunction<*>? {
    // As an optimization, try to search among functions with the same name first, and then among the rest of functions.
    // This is needed because a function's JVM name might be different from its Kotlin name (because of `@JvmName`, inline class mangling,
    // internal visibility, etc).
    for (callable in this) {
        if (callable is KFunction<*> && callable.name == method.name && callable.javaMethod == method) return callable
    }
    for (callable in this) {
        if (callable is KFunction<*> && callable.name != method.name && callable.javaMethod == method) return callable
    }
    return null
}

private fun Collection<KCallable<*>>.findKProperty(field: Field): KProperty<*>? {
    for (callable in this) {
        if (callable is KProperty<*> && callable.name == field.name && callable.javaField == field) return callable
    }
    for (callable in this) {
        if (callable is KProperty<*> && callable.name != field.name && callable.javaField == field) return callable
    }
    return null
}

/**
 * Returns a [KFunction] instance corresponding to the given Java [Constructor] instance,
 * or `null` if this constructor cannot be represented by a Kotlin function
 * (for example, if it is a synthetic constructor).
 */
val <T : Any> Constructor<T>.kotlinFunction: KFunction<T>?
    get() {
        return declaringClass.kotlin.constructors.firstOrNull { it.javaConstructor == this }
    }
