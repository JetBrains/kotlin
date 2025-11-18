/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.reflect.Constructor
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.createDefaultType

internal abstract class ReflectKParameter : KParameter {
    abstract val callable: ReflectKCallable<*>

    abstract val declaresDefaultValue: Boolean

    override val annotations: List<Annotation> by lazy(PUBLICATION) {
        val java = javaParameter
        when (val callable = java?.callable) {
            is Method -> callable.parameterAnnotations[java.index].toList()
            is Constructor<*> -> callable.parameterAnnotations[java.index].toList()
            else -> emptyList()
        }.unwrapKotlinRepeatableAnnotations()
    }

    final override fun equals(other: Any?): Boolean =
        other is ReflectKParameter && callable == other.callable && index == other.index

    final override fun hashCode(): Int =
        (callable.hashCode() * 31) + index.hashCode()

    final override fun toString(): String =
        ReflectionObjectRenderer.renderParameter(this)
}

internal class InstanceParameter(override val callable: ReflectKCallable<*>, klass: KClass<*>) : ReflectKParameter() {
    override val index: Int get() = 0
    override val type: KType = klass.createDefaultType()
    override val name: String? get() = null
    override val kind: KParameter.Kind get() = KParameter.Kind.INSTANCE
    override val isOptional: Boolean get() = false
    override val isVararg: Boolean get() = false
    override val annotations: List<Annotation> get() = emptyList()
    override val declaresDefaultValue: Boolean get() = false
}

/**
 * Represents a parameter in Java reflection. Unfortunately, there's no good representation of parameters in Java reflection, and we can't
 * use [java.lang.reflect.Parameter] because it's only available with javac's `-parameters` (or Kotlin's `-java-parameters`) option,
 * and requires API level >= 26 on Android.
 *
 * @param callable method or constructor, which this parameter belongs to.
 * @param index index of this parameter in [callable]'s parameter list. Note that dispatch receiver parameter is not a part of parameters
 *   in Java reflection, so the index goes only through context, extension receiver, and value parameters.
 */
internal class JavaParameter(val callable: Member, val index: Int)

internal val ReflectKParameter.javaParameter: JavaParameter?
    get() = when (val callable = callable.caller.member) {
        is Method -> {
            require(Modifier.isStatic(callable.modifiers)) { "Only static methods are supported for now: $callable" }
            JavaParameter(callable, index)
        }
        is Constructor<*> -> {
            val shift = when {
                // Inner class constructors before JDK 9 did not have the outer class parameter in `parameterAnnotations`, see
                // https://bugs.java.com/bugdatabase/view_bug?bug_id=8074977.
                callable.declaringClass.kotlin.isInner && isJdk8() -> -1
                // Enum constructors before JDK 17 did not have additional name/ordinal parameters in case there was at least one annotation
                // on any constructor parameter. (Probably some fixed bug in the JDK as well.)
                callable.declaringClass.isEnum -> callable.parameterAnnotations.size - callable.parameterTypes.size + 2
                else -> 0
            }
            JavaParameter(callable, index + shift)
        }
        else -> throw KotlinReflectionInternalError("Unsupported parameter owner: $callable")
    }

private fun isJdk8(): Boolean =
    System.getProperty("java.version")?.startsWith("1.") == true
