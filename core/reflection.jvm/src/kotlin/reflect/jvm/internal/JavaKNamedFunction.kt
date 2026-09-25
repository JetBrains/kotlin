/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.runtime.structure.classId
import org.jetbrains.kotlin.load.java.typeEnhancement.PREDEFINED_FUNCTION_ENHANCEMENT_INFO_BY_SIGNATURE
import org.jetbrains.kotlin.load.java.typeEnhancement.PredefinedFunctionEnhancementInfo
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.load.kotlin.internalName
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.jvm.internal.calls.Caller
import kotlin.reflect.jvm.internal.calls.CallerImpl
import kotlin.reflect.jvm.internal.types.AbstractKType

internal class JavaKNamedFunction(
    container: KDeclarationContainerImpl,
    method: Method,
    rawBoundReceiver: Any?,
    overriddenStorage: KCallableOverriddenStorage,
) : JavaKFunction(container, method, rawBoundReceiver, overriddenStorage) {
    override val originalParameters: List<KParameter> by lazy(PUBLICATION) {
        computeParameters()
    }

    override val originalReturnType: AbstractKType by lazy(PUBLICATION) {
        val unsubstitutedReturnType =
            if (overriddenStorage.isFakeOverride && overriddenStorage.overridden.size == 1) {
                overriddenStorage.overridden.single().returnType
            } else {
                jMethod.genericReturnType.toKType(
                    javaTypeParameters.zip(typeParameters).toMap(),
                    // Return type of enum values/valueOf methods is not flexible in the compiler even for Java enums, so we use the annotation
                    // parameter mapping mode, which removes all flexibility.
                    isForAnnotationParameter = member.isEnumValuesValueOfMethod(),
                )
            }
        substituteType(unsubstitutedReturnType) as AbstractKType
    }

    // Predefined enhancement of well-known JDK methods (e.g. `Iterator.forEachRemaining`, whose `Consumer<in T>` parameter must not be
    // flexible). It's applied only in the "errors" mode; the "warnings-only" entries don't change the type. See
    // `SignatureEnhancement.enhanceSignature` in the compiler.
    override val predefinedEnhancementInfo: PredefinedFunctionEnhancementInfo?
        get() = PREDEFINED_FUNCTION_ENHANCEMENT_INFO_BY_SIGNATURE[
            SignatureBuildingComponents.signature(jMethod.declaringClass.classId.internalName, jMethod.jvmSignature)
        ]?.takeIf { it.errorsSinceLanguageVersion == null }

    override fun computeOverriddenFunctionsForEnhancement(): Collection<ReflectKFunction>? {
        if (Modifier.isStatic(jMethod.modifiers)) return emptyList()
        val signature = toEquatableCallableSignature(EqualityMode.KotlinSignature)
        val overridden = computeOverriddenFunctions(container as KClassImpl<*>, signature)
        if (overriddenStorage.isFakeOverride && overridden.size == 1) return null
        return overridden
    }

    val jMethod: Method get() = member as Method

    override val name: String
        get() = member.name

    override val signature: String
        get() = jMethod.jvmSignature

    override val parameterTypes: Array<out Class<*>>
        get() = jMethod.parameterTypes

    override val genericParameterTypes: Array<Type>
        get() = jMethod.genericParameterTypes

    override val isVararg: Boolean
        get() = jMethod.isVarArgs

    override val isOperator: Boolean by lazy(PUBLICATION, jMethod::isJavaMethodAnOperator)

    override val javaTypeParameters: Array<out TypeVariable<*>> by lazy(PUBLICATION) {
        jMethod.typeParameters
    }

    override val returnType: KType
        get() = enhancedSignature?.returnType ?: originalReturnType

    override val isPrimaryConstructor: Boolean get() = false

    override val overridden: Collection<ReflectKFunction> by lazy(PUBLICATION) {
        computeOverriddenFunctions(this)
    }

    override val allParameters: List<KParameter>
        get() = enhancedSignature?.allParameters ?: originalParameters

    override val caller: Caller<*> by lazy(PUBLICATION) {
        if (Modifier.isStatic(jMethod.modifiers)) createStaticMethodCaller(jMethod)
        else createInstanceMethodCaller(jMethod)
    }

    private fun createInstanceMethodCaller(member: Method): Caller<*> =
        if (isBound) CallerImpl.Method.BoundInstance(member, boundReceiver)
        else CallerImpl.Method.Instance(member)

    private fun createStaticMethodCaller(member: Method): Caller<*> =
        if (isBound)
            CallerImpl.Method.BoundStatic(member, isCallByToValueClassMangledMethod = false, boundReceiver)
        else CallerImpl.Method.Static(member)

    override val callerWithDefaults: Caller<*>? get() = null

    override fun shallowCopy(container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage): ReflectKCallable<Any?> =
        JavaKNamedFunction(container, jMethod, CallableReference.NO_RECEIVER, overriddenStorage)

    override fun rebind(boundReceiver: Any?): ReflectKCallable<Any?> =
        if (this.rawBoundReceiver === boundReceiver) this
        else JavaKNamedFunction(container, jMethod, boundReceiver, overriddenStorage)
}
