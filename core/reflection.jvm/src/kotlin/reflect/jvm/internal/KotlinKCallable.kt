/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.name.SpecialNames
import kotlin.metadata.KmType
import kotlin.metadata.KmValueParameter
import kotlin.reflect.KParameter

internal abstract class KotlinKCallable<out R>(
    overriddenStorage: KCallableOverriddenStorage,
) : ReflectKCallableImpl<R>(overriddenStorage) {
    abstract override val rawBoundReceiver: Any?

    abstract override val annotations: List<Annotation>

    final override val isPackagePrivate: Boolean get() = false
}

private val KotlinKCallable<*>.isLocalDelegatedProperty: Boolean
    get() = this is KotlinKProperty<*> && isLocalDelegated

internal fun KotlinKCallable<*>.computeParameters(
    contextParameters: List<KmValueParameter>,
    receiverParameterType: KmType?,
    valueParameters: List<KmValueParameter>,
    typeParameterTable: TypeParameterTable,
    includeReceivers: Boolean,
): List<KParameter> = buildList {
    val callable = this@computeParameters
    if (includeReceivers) {
        val container = container
        if (container is KClassImpl<*>) {
            if (isConstructor) {
                if (container.isInner) {
                    add(InstanceParameter(callable, container.java.declaringClass.kotlin))
                }
            } else {
                require(isLocalDelegatedProperty) {
                    "Only top-level callables are supported for now: ${this@computeParameters}"
                }
            }
        }
        for (contextParameter in contextParameters) {
            add(KotlinKParameter(callable, contextParameter, size, KParameter.Kind.CONTEXT, typeParameterTable))
        }
        if (receiverParameterType != null) {
            // The name below is only used to create an instance of `KmValueParameter`. It should not leak to the user, because
            // `KotlinKParameter.name` returns null if the name is special (starts with a `<`).
            val kmParameter = KmValueParameter(SpecialNames.THIS.asString()).apply { type = receiverParameterType }
            add(KotlinKParameter(callable, kmParameter, size, KParameter.Kind.EXTENSION_RECEIVER, typeParameterTable))
        }
    }
    for (valueParameter in valueParameters) {
        add(KotlinKParameter(callable, valueParameter, size, KParameter.Kind.VALUE, typeParameterTable))
    }
}
