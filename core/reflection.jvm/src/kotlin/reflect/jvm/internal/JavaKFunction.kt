/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.runtime.structure.Java8ParameterNamesLoader
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Member
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.jvm.internal.FunctionBase
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.internal.calls.arity

internal abstract class JavaKFunction(
    container: KDeclarationContainerImpl,
    member: Member,
    rawBoundReceiver: Any?,
) : JavaKCallable<Any?>(container, member, rawBoundReceiver), ReflectKFunction, FunctionBase<Any?>, FunctionWithAllInvokes {
    abstract val genericParameterTypes: Array<Type>

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

private fun JavaKFunction.computeParameters(includeReceivers: Boolean): List<KParameter> {
    require(this is JavaKConstructor) { "Only Java constructors are supported for now: $this" }

    val isInnerClassConstructor = jConstructor.declaringClass.isInner

    val names = Java8ParameterNamesLoader.loadParameterNames(member)
    // Skip synthetic parameters, such as outer class instance and enum name/ordinal.
    val shift = names?.size?.minus(genericParameterTypes.size) ?: 0

    val parameterAnnotations = jConstructor.parameterAnnotations.run {
        // Despite its javadoc, `parameterAnnotations` does not include the outer class instance parameter.
        if (isInnerClassConstructor) {
            Array(size + 1) { i -> getOrNull(i - 1).orEmpty() }
        } else this
    }

    // TODO (KT-82659): fix calls for Java inner class constructors and make sure they work in the new implementation as well.
    val result = mutableListOf<KParameter>()
    for ((i, type) in genericParameterTypes.withIndex()) {
        val name = when {
            isInnerClassConstructor && i == 0 -> null
            names != null ->
                names.getOrNull(i + shift) ?: error("No parameter with index $i+$shift (name=$name type=$type) in $member")
            else -> "arg$i"
        }
        result.add(
            JavaKParameter(
                this, name, type.toKType(emptyMap()), i, KParameter.Kind.VALUE,
                isVararg = i == genericParameterTypes.lastIndex && jConstructor.isVarArgs,
                parameterAnnotations[i].toList(),
            )
        )
    }
    return result.toList()
}

private val Class<*>.isInner: Boolean
    get() = declaringClass != null && !Modifier.isStatic(modifiers)
