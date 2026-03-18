/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.reflect.KType
import kotlin.reflect.jvm.internal.calls.Caller
import kotlin.reflect.jvm.internal.calls.CallerImpl

internal class JavaKNamedFunction(
    container: KDeclarationContainerImpl,
    method: Method,
    rawBoundReceiver: Any?,
    overriddenStorage: KCallableOverriddenStorage,
) : JavaKFunction(container, method, rawBoundReceiver, overriddenStorage) {
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

    override val javaTypeParameters: Array<out TypeVariable<*>> by lazy(PUBLICATION) {
        jMethod.typeParameters
    }

    override val returnType: KType by lazy(PUBLICATION) {
        // Return type of enum values/valueOf methods is not flexible in the compiler even for Java enums, so we use the annotation
        // parameter mapping mode, which removes all flexibility.
        jMethod.genericReturnType.toKType(emptyMap(), isForAnnotationParameter = member.isEnumValuesValueOfMethod())
    }

    override val isPrimaryConstructor: Boolean get() = false

    override val overridden: Collection<ReflectKFunction>
        get() {
            require(Modifier.isStatic(jMethod.modifiers)) {
                "Only static Java methods are supported for now: $member"
            }
            return emptyList()
        }

    override val caller: Caller<*> by lazy(PUBLICATION) {
        require(Modifier.isStatic(jMethod.modifiers)) {
            "Only static Java methods are supported for now: $member"
        }
        createStaticMethodCaller(jMethod)
    }

    private fun createStaticMethodCaller(member: Method): Caller<*> =
        if (isBound)
            CallerImpl.Method.BoundStatic(member, isCallByToValueClassMangledMethod = false, boundReceiver)
        else CallerImpl.Method.Static(member)

    override val callerWithDefaults: Caller<*>? get() = null

    override fun replaceContainerForFakeOverride(
        container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage,
    ): ReflectKCallable<Any?> =
        JavaKNamedFunction(container, jMethod, rawBoundReceiver, overriddenStorage)
}
