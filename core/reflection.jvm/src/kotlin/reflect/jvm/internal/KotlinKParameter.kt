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
import kotlin.metadata.KmValueParameter
import kotlin.metadata.declaresDefaultValue
import kotlin.reflect.KParameter
import kotlin.reflect.KType

internal class KotlinKParameter(
    override val callable: KotlinKCallable<*>,
    private val kmParameter: KmValueParameter,
    override val index: Int,
    override val kind: KParameter.Kind,
    typeParameterTable: TypeParameterTable,
) : ReflectKParameter() {
    override val name: String? =
        kmParameter.name.takeUnless { kind == KParameter.Kind.EXTENSION_RECEIVER }

    override val type: KType by lazy(PUBLICATION) {
        kmParameter.type.toKType(callable.container.jClass.classLoader, typeParameterTable) {
            require(callable.container is KPackageImpl) {
                // For class callables, we'll also need to tweak instance receiver parameter type (see `DescriptorKParameter`).
                "Only top-level callables are supported for now: $callable"
            }
            callable.caller.parameterTypes[index]
        }
    }

    override val isOptional: Boolean
        get() {
            require(callable is KotlinKProperty<*> || callable.container is KPackageImpl) {
                // For class functions, we'll also need to check the flag for parameters from inherited functions.
                "Only top-level callables are supported for now: $callable"
            }
            return kmParameter.declaresDefaultValue
        }

    override val declaresDefaultValue: Boolean
        get() = kmParameter.declaresDefaultValue

    override val isVararg: Boolean
        get() = kmParameter.varargElementType != null

    override val annotations: List<Annotation> by lazy(PUBLICATION) {
        val java = javaParameter
        when (val callable = java?.callable) {
            is Method -> callable.parameterAnnotations[java.index].toList()
            is Constructor<*> -> callable.parameterAnnotations[java.index].toList()
            else -> emptyList()
        }.unwrapKotlinRepeatableAnnotations()
    }
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
private class JavaParameter(val callable: Member, val index: Int)

private val KotlinKParameter.javaParameter: JavaParameter?
    get() {
        return when (val callable = callable.caller.member) {
            is Method -> {
                require(Modifier.isStatic(callable.modifiers)) { "Only static methods are supported for now: $callable" }
                JavaParameter(callable, index)
            }
            else -> error("Only methods are supported for now: $callable")
        }
    }
