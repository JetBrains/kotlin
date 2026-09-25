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
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.internal.calls.Caller
import kotlin.reflect.jvm.internal.calls.CallerImpl
import kotlin.reflect.jvm.internal.types.AbstractKType

/**
 * @param kotlinName the Kotlin name of the function if it differs from the name of the Java method, e.g. `removeAt` for `remove(int)` in a
 *   Java implementation of `MutableList`, see `addOverriddenSpecialMethods`.
 * @param erasedValueParameterTypesFrom the built-in function whose value parameter types this function has, if this Java method overrides
 *   a built-in function with erased value parameters in Java, e.g. `contains(E)` for `contains(Object)` in a Java implementation of
 *   `Collection`. The built-in function is substituted to the type parameters of the class containing this function.
 */
internal class JavaKNamedFunction(
    container: KDeclarationContainerImpl,
    method: Method,
    rawBoundReceiver: Any?,
    overriddenStorage: KCallableOverriddenStorage,
    private val kotlinName: String? = null,
    val erasedValueParameterTypesFrom: ReflectKFunction? = null,
) : JavaKFunction(container, method, rawBoundReceiver, overriddenStorage) {
    override val valueParameterTypesOverride: List<KType>?
        get() = erasedValueParameterTypesFrom?.valueParameters?.map { it.type }

    override val originalParameters: List<KParameter> by lazy(PUBLICATION) {
        computeParameters()
    }

    override val originalReturnType: AbstractKType by lazy(PUBLICATION) {
        val unsubstitutedReturnType =
            overriddenCallableToInheritSignature?.returnType
                ?: jMethod.genericReturnType.toKType(
                    javaTypeParameters.zip(typeParameters).toMap(),
                    // Return type of enum values/valueOf methods is not flexible in the compiler even for Java enums, so we use the annotation
                    // parameter mapping mode, which removes all flexibility.
                    isForAnnotationParameter = member.isEnumValuesValueOfMethod(),
                )
        substituteType(unsubstitutedReturnType) as AbstractKType
    }

    // Predefined enhancement of well-known JDK methods (e.g. `Iterator.forEachRemaining`, whose `Consumer<in T>` parameter must not be
    // flexible). It's applied only in the "errors" mode; the "warnings-only" entries don't change the type. See
    // `SignatureEnhancement.enhanceSignature` in the compiler.
    // It applies only to the declaration itself, not to its fake overrides in subclasses, which are enhanced by the (already enhanced)
    // signatures of the overridden functions instead.
    override val predefinedEnhancementInfo: PredefinedFunctionEnhancementInfo?
        get() = if (overriddenStorage.isFakeOverride) null else PREDEFINED_FUNCTION_ENHANCEMENT_INFO_BY_SIGNATURE[
            SignatureBuildingComponents.signature(jMethod.declaringClass.classId.internalName, jMethod.jvmSignature)
        ]?.takeIf { it.errorsSinceLanguageVersion == null }

    override fun computeOverriddenFunctionsForEnhancement(supertypes: List<KType>?): Collection<ReflectKFunction>? {
        if (Modifier.isStatic(jMethod.modifiers)) return emptyList()
        val signature = toEquatableCallableSignature(EqualityMode.KotlinSignature)
        // The overridden functions are substituted to the type parameters of this class, because their types are compared with and applied
        // to the types of this function. For example, `get(): E` in `AbstractMutableList<Int!>` becomes `get(): Int!` and thus does not
        // enhance the return type of the overriding Java method `Integer get(int)`.
        // Note that the substituted copies are fake overrides in this class, so we must take their unenhanced types (which are the
        // substituted types of the original overridden functions) to avoid infinite recursion in case this function is a fake override too.
        val overridden = if (supertypes != null) {
            // The only functions in Kotlin classes which are enhanced are additional built-in members (see `getAdditionalFunctions`). They
            // are enhanced by the predefined enhancement info, and by the signatures of other additional members they override, which are
            // found through the supertypes of the Java analogue class, e.g. `java.util.Collection.spliterator` is enhanced by
            // `java.lang.Iterable.spliterator` which has a predefined enhancement.
            computeOverriddenFunctions(container as MemberContainer<*>, signature, substituted = true, supertypes)
        } else {
            computeOverriddenFunctions(container as MemberContainer<*>, signature, substituted = true).also {
                if (overriddenStorage.isFakeOverride && overridden.size == 1) return null
            }
        }
        return overridden
    }

    val jMethod: Method get() = member as Method

    override val name: String
        get() = kotlinName ?: member.name

    override val signature: String
        get() = jMethod.jvmSignature

    override val parameterTypes: Array<out Class<*>>
        get() = jMethod.parameterTypes

    override val genericParameterTypes: Array<Type>
        get() = jMethod.genericParameterTypes

    override val isVararg: Boolean
        get() = jMethod.isVarArgs

    override val isOperator: Boolean by lazy(PUBLICATION) { jMethod.isJavaMethodAnOperator(name) }

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
        if (Modifier.isStatic(jMethod.modifiers))
            CallerImpl.Method.Static(
                jMethod, isCallByToValueClassMangledMethod = false, boundReceiver,
                boundContextArguments = emptyArray(), hasInstanceParameter = false,
            )
        else
            CallerImpl.Method.Instance(jMethod, boundReceiver, boundContextArguments = emptyArray())
    }

    override val callerWithDefaults: Caller<*>? get() = null

    override fun shallowCopy(container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage): ReflectKCallable<Any?> =
        JavaKNamedFunction(container, jMethod, CallableReference.NO_RECEIVER, overriddenStorage, kotlinName, erasedValueParameterTypesFrom)

    override fun bindToLowerArity(boundReceiver: Any?, boundContextArguments: List<Any?>): ReflectKCallable<Any?> {
        require(boundContextArguments.isEmpty()) { "Java methods cannot have bound context arguments: $this" }
        return JavaKNamedFunction(container, jMethod, boundReceiver, overriddenStorage, kotlinName, erasedValueParameterTypesFrom)
    }
}
