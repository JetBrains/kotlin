/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.reflect.Constructor
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.createDefaultType
import kotlin.reflect.jvm.internal.calls.Caller
import kotlin.reflect.jvm.internal.calls.CallerImpl

internal class JavaKConstructor(
    container: KDeclarationContainerImpl,
    constructor: Constructor<*>,
    rawBoundReceiver: Any?,
) : JavaKFunction(container, constructor, rawBoundReceiver, KCallableOverriddenStorage.EMPTY) {
    private val jClass: Class<*> get() = container.jClass
    val jConstructor: Constructor<*> get() = member as Constructor<*>

    override val name: String
        get() = "<init>"

    override val signature: String
        get() = jConstructor.jvmSignature

    override val parameterTypes: Array<out Class<*>>
        get() = jConstructor.parameterTypes

    override val genericParameterTypes: Array<Type>
        get() = jConstructor.genericParameterTypes

    override val isVararg: Boolean
        get() = jConstructor.isVarArgs

    override val typeParameters: List<KTypeParameter> by lazy(PUBLICATION) {
        @Suppress("UNCHECKED_CAST")
        val classTypeParameters = jClass.typeParameters as Array<TypeVariable<*>>
        val constructorTypeParameters = jConstructor.typeParameters
        (classTypeParameters + constructorTypeParameters).toKTypeParameters()
    }

    override val returnType: KType by lazy(PUBLICATION) {
        (container as KClassImpl<*>).createDefaultType()
    }

    override val isPrimaryConstructor: Boolean get() = false

    override val overridden: Collection<ReflectKFunction> = emptyList()

    override val caller: Caller<*> by lazy(PUBLICATION) {
        if (isBound) CallerImpl.BoundConstructor(jConstructor, boundReceiver)
        else CallerImpl.Constructor(jConstructor)
    }

    override val callerWithDefaults: Caller<*>? get() = null

    override fun shallowCopy(container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage): ReflectKCallable<Any?> =
        error("Constructors cannot be copied: $this")
}
