/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.runtime.structure.Java8ParameterNamesLoader
import java.lang.reflect.*
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.jvm.internal.FunctionBase
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.internal.calls.arity

internal abstract class JavaKFunction(
    container: KDeclarationContainerImpl,
    member: Member,
    rawBoundReceiver: Any?,
    overriddenStorage: KCallableOverriddenStorage,
) : JavaKCallable<Any?>(container, member, rawBoundReceiver, overriddenStorage),
    ReflectKFunction, FunctionBase<Any?>, FunctionWithAllInvokes {
    abstract val parameterTypes: Array<out Class<*>>
    abstract val genericParameterTypes: Array<Type>
    abstract val isVararg: Boolean

    override val allParameters: List<KParameter> by lazy(PUBLICATION) {
        computeParameters(includeReceivers = true)
    }

    override val parameters: List<KParameter> by lazy(PUBLICATION) {
        if (isBound) computeParameters(includeReceivers = false)
        else allParameters
    }

    override val annotations: List<Annotation>
        get() {
            val member = caller.member as? AnnotatedElement ?: return emptyList()
            return member.annotations.toList().unwrapKotlinRepeatableAnnotations()
        }

    override val arity: Int get() = caller.arity

    override val isInline: Boolean get() = false
    override val isExternal: Boolean get() = Modifier.isNative(member.modifiers)
    override val isOperator: Boolean get() = false
    override val isInfix: Boolean get() = false

    override fun equals(other: Any?): Boolean {
        val that = other.asReflectFunction() ?: return false
        return container == that.container && name == that.name && signature == that.signature && rawBoundReceiver == that.rawBoundReceiver
    }

    override fun hashCode(): Int =
        (container.hashCode() * 31 + name.hashCode()) * 31 + signature.hashCode()

    override fun toString(): String =
        ReflectionObjectRenderer.renderFunction(this)
}

private fun JavaKFunction.computeParameters(includeReceivers: Boolean): List<KParameter> = buildList {
    val function = this@computeParameters
    require(function is JavaKConstructor) { "Only Java constructors are supported for now: $this" }

    val isInnerClassConstructor = member is Constructor<*> && member.declaringClass.isInner
    val genericParameterTypes = genericParameterTypes

    if (includeReceivers) {
        if (isInnerClassConstructor) {
            add(InstanceParameter(function, member.declaringClass.declaringClass.kotlin))
        }
    }

    val names = Java8ParameterNamesLoader.loadParameterNames(member)
    // Skip synthetic parameters, such as outer class instance and enum name/ordinal.
    val shift = names?.size?.minus(genericParameterTypes.size) ?: 0

    for ((i, type) in genericParameterTypes.withIndex()) {
        // If constructor is generic, its `genericParameterTypes` does not have the outer class instance parameter.
        // If it's not generic, `genericParameterTypes` delegates to `parameterTypes`, which has the outer class instance parameter. We need
        // to skip this parameter because we've added it as `InstanceParameter` above.
        if (i == 0 && isInnerClassConstructor && genericParameterTypes.size == parameterTypes.size) continue

        // Normally, enum name/ordinal parameters are absent in `genericParameterTypes`, however for some reason, they are present for
        // classes compiled by Groovy (see the test `SimpleKotlinGradleIT.testGroovyInterop`). In this case, we must manually skip them.
        // We detect this case by the fact that `genericParameterTypes` and `parameterTypes` have the same size (note that `parameterTypes`
        // always has the types of name/ordinal, `String` and `int`).
        if (i < 2 && member.declaringClass.isEnum && genericParameterTypes.size == parameterTypes.size) continue

        val name = when {
            names != null -> names.getOrNull(i + shift) ?: error("No parameter with index $i+$shift (name=$name type=$type) in $member")
            else -> "arg$i"
        }
        add(
            JavaKParameter(
                function, name, type.toKType(emptyMap()), size, KParameter.Kind.VALUE,
                isVararg = i == genericParameterTypes.lastIndex && isVararg,
            )
        )
    }
}

private val Class<*>.isInner: Boolean
    get() = declaringClass != null && !Modifier.isStatic(modifiers)
