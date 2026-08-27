/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.runtime.structure.Java8ParameterNamesLoader
import org.jetbrains.kotlin.load.java.AnnotationQualifierApplicabilityType.METHOD_RETURN_TYPE
import org.jetbrains.kotlin.load.java.AnnotationQualifierApplicabilityType.VALUE_PARAMETER
import org.jetbrains.kotlin.load.java.typeEnhancement.PredefinedFunctionEnhancementInfo
import java.lang.reflect.*
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.jvm.internal.FunctionBase
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.jvm.internal.calls.arity
import kotlin.reflect.jvm.internal.types.AbstractKType

internal abstract class JavaKFunction(
    container: KDeclarationContainerImpl,
    member: Member,
    rawBoundReceiver: Any?,
    overriddenStorage: KCallableOverriddenStorage,
) : JavaKCallable<Any?>(container, member, rawBoundReceiver, overriddenStorage),
    ReflectKFunction, FunctionBase<Any?>, FunctionWithAllInvokes {
    abstract val parameterTypes: Array<out Class<*>>
    abstract val genericParameterTypes: Array<Type>
    abstract val javaTypeParameters: Array<out TypeVariable<*>>
    abstract val isVararg: Boolean

    abstract val originalParameters: List<KParameter>

    open val originalReturnType: AbstractKType?
        get() = null

    protected open val predefinedEnhancementInfo: PredefinedFunctionEnhancementInfo?
        get() = null

    // Returns null if the signature of this function should not be enhanced.
    protected open fun computeOverriddenFunctionsForEnhancement(): Collection<ReflectKFunction>? = emptyList()

    protected val enhancedSignature: EnhancedSignature? by lazy(PUBLICATION) {
        val predefinedEnhancementInfo = predefinedEnhancementInfo

        // Callables in Kotlin classes (even fake overrides of Java methods) are not enhanced from supertypes/nullability annotations.
        // Only the predefined enhancement of additional built-in members applies to them (see `getAdditionalFunctions`).
        val isKotlinContainer = (container as KClassImpl<*>).kmClass != null
        if (isKotlinContainer && predefinedEnhancementInfo == null) return@lazy null

        val overridden = (if (isKotlinContainer) emptyList() else computeOverriddenFunctionsForEnhancement())
            ?: return@lazy null

        val enhancedReturnType = originalReturnType?.let { originalReturnType ->
            val returnTypeAnnotations =
                if (isKotlinContainer) emptyList() else (member as Method).declaredAnnotations.toList()
            with(ReflectSignatureParts(METHOD_RETURN_TYPE, returnTypeAnnotations)) {
                val qualifiers = originalReturnType.computeIndexedQualifiers(
                    overridden.map { it.returnType as AbstractKType }, predefinedEnhancementInfo?.returnTypeInfo,
                )
                originalReturnType.enhance(qualifiers)
            }
        }

        val parameterAnnotations: Array<Array<Annotation>>? = when {
            isKotlinContainer -> null
            member is Method -> member.parameterAnnotations
            member is Constructor<*> -> member.parameterAnnotations
            else -> null
        }

        val hasInstanceParameter = originalParameters.firstOrNull() is InstanceParameter

        var valueParameterIndex = 0
        val enhancedParameters = originalParameters.map { p ->
            // Dispatch receiver parameter (InstanceParameter) type cannot be enhanced.
            if (p !is JavaKParameter) return@map p

            // `parametersInfo` is indexed by value parameter, while `p.index` also counts the instance parameter.
            val predefinedParameterInfo = predefinedEnhancementInfo?.parametersInfo?.getOrNull(valueParameterIndex++)
            val annotations = parameterAnnotations
                ?.getOrNull(computeJavaParameterAnnotationIndexWithWorkarounds(member, p.index, hasInstanceParameter))
                ?.toList().orEmpty()
            with(ReflectSignatureParts(VALUE_PARAMETER, annotations, containerIsVarargParameter = p.isVararg)) {
                val type = p.type as AbstractKType
                val qualifiers = type.computeIndexedQualifiers(
                    overridden.map { it.parameters[p.index].type as AbstractKType }, predefinedParameterInfo,
                )
                val enhancedType = type.enhance(qualifiers)
                if (type === enhancedType) p
                else JavaKParameter(p.callable, p.name, enhancedType, p.index, p.kind, p.isVararg)
            }
        }

        // Java type parameter bounds are not enhanced from annotations because JSR-305 type qualifier defaults
        // (e.g. `@ParametersAreNonnullByDefault`) are not supported in kotlin-reflect, and nullability annotations directly on type
        // parameter declarations are not loaded via `getParameterAnnotations`/`getDeclaredAnnotations`.
        EnhancedSignature(enhancedParameters, enhancedReturnType)
    }

    protected class EnhancedSignature(
        val allParameters: List<KParameter>,
        val returnType: KType?,
    )

    override val parameters: List<KParameter> by lazy(PUBLICATION) {
        val allParameters = allParameters
        if (!isBound) return@lazy allParameters
        // For bound references, recreate all parameters except the bound one, with the correct indices.
        check(allParameters.isNotEmpty()) { "Bound function reference has no parameters: $container.$name" }
        List(allParameters.size - 1) { i ->
            val parameter = allParameters[i + 1]
            check(parameter is JavaKParameter) { "Unexpected parameter type: ${parameter::class.simpleName} ($container.$name)" }
            JavaKParameter(parameter.callable, parameter.name, parameter.type, i, parameter.kind, parameter.isVararg)
        }
    }

    override val typeParameters: List<KTypeParameter> by lazy(PUBLICATION) {
        javaTypeParameters.toKTypeParameters(this)
    }

    override val arity: Int get() = caller.arity

    override val isInline: Boolean get() = false
    override val isExternal: Boolean get() = Modifier.isNative(member.modifiers)
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

internal fun JavaKFunction.computeParameters(): List<KParameter> = buildList {
    val function = this@computeParameters

    val isInnerClassConstructor = member is Constructor<*> && member.declaringClass.isInner
    val knownTypeParameters = javaTypeParameters.zip(typeParameters).toMap()

    val unsubstitutedParameterKTypes =
        if (overriddenStorage.isFakeOverride && overriddenStorage.overridden.size == 1)
            overriddenStorage.overridden.single().parameters.filter { it.kind == KParameter.Kind.VALUE }.map { it.type }
        else
            genericParameterTypes.map { type ->
                val nullability = if (member.isEnumValuesValueOfMethod()) TypeNullability.NOT_NULL else TypeNullability.FLEXIBLE
                type.toKType(knownTypeParameters, nullability)
            }

    val parameterKTypes = unsubstitutedParameterKTypes.map(function::substituteType)

    if (isInnerClassConstructor) {
        add(InstanceParameter(function, member.declaringClass.declaringClass.kotlin))
    } else if (member is Method && !Modifier.isStatic(member.modifiers)) {
        add(InstanceParameter(function, container as KClassImpl<*>))
    }

    val names = Java8ParameterNamesLoader.loadParameterNames(member)
    // Skip synthetic parameters, such as outer class instance and enum name/ordinal.
    val shift = names?.size?.minus(parameterKTypes.size) ?: 0

    for ((i, type) in parameterKTypes.withIndex()) {
        // If constructor is generic, its `genericParameterTypes` does not have the outer class instance parameter.
        // If it's not generic, `genericParameterTypes` delegates to `parameterTypes`, which has the outer class instance parameter. We need
        // to skip this parameter because we've added it as `InstanceParameter` above.
        if (i == 0 && isInnerClassConstructor && parameterKTypes.size == parameterTypes.size) continue

        // Normally, enum name/ordinal parameters are absent in `genericParameterTypes`, however for some reason, they are present for
        // classes compiled by Groovy (see the test `SimpleKotlinGradleIT.testGroovyInterop`). In this case, we must manually skip them.
        // We detect this case by the fact that `genericParameterTypes` and `parameterTypes` have the same size (note that `parameterTypes`
        // always has the types of name/ordinal, `String` and `int`).
        if (i < 2 && member.declaringClass.isEnum && member is Constructor<*> && parameterKTypes.size == parameterTypes.size) continue

        val name = when {
            names != null -> names.getOrNull(i + shift) ?: error("No parameter with index $i+$shift (name=$name type=$type) in $member")
            else -> "arg$i"
        }

        val isVararg = i == parameterKTypes.lastIndex && isVararg
        add(JavaKParameter(function, name, type, size, KParameter.Kind.VALUE, isVararg = isVararg))
    }
}

private val Class<*>.isInner: Boolean
    get() = declaringClass != null && !Modifier.isStatic(modifiers)
