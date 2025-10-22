/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import kotlin.metadata.Modality
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.internal.calls.Caller
import kotlin.reflect.jvm.internal.calls.ThrowingCaller

internal abstract class KotlinKCallable<out R> : ReflectKCallable<R> {
    abstract val modality: Modality
    abstract override val rawBoundReceiver: Any?

    final override val isFinal: Boolean
        get() = modality == Modality.FINAL

    final override val isOpen: Boolean
        get() = modality == Modality.OPEN

    final override val isAbstract: Boolean
        get() = modality == Modality.ABSTRACT

    override val parameters: List<KParameter>
        get() {
            checkLocalDelegatedPropertyOrAccessor()
            require(allParameters.all { it.kind == KParameter.Kind.VALUE }) {
                "Local delegated properties and their accessors can only have value parameters"
            }
            return allParameters
        }

    abstract override val annotations: List<Annotation>

    private val _absentArguments = ReflectProperties.lazySoft(::computeAbsentArguments)

    override fun getAbsentArguments(): Array<Any?> = _absentArguments().clone()

    override val caller: Caller<*>
        get() {
            checkLocalDelegatedPropertyOrAccessor()
            return ThrowingCaller
        }

    override val defaultCaller: Caller<*>?
        get() {
            checkLocalDelegatedPropertyOrAccessor()
            return ThrowingCaller
        }

    override fun callBy(args: Map<KParameter, Any?>): R {
        checkLocalDelegatedPropertyOrAccessor()
        return callDefaultMethod(args, null)
    }
}

internal fun KotlinKCallable<*>.checkLocalDelegatedPropertyOrAccessor() {
    require(isLocalDelegatedPropertyOrAccessor) {
        "Only local delegated properties can be descriptor-less for now"
    }
}

private val KotlinKCallable<*>.isLocalDelegatedPropertyOrAccessor: Boolean
    get() = when (this) {
        is KotlinKProperty<*> -> isLocalDelegated
        is KotlinKProperty.Accessor<*, *> -> property.isLocalDelegated
        else -> false
    }
