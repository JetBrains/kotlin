/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import kotlin.metadata.KmType
import kotlin.metadata.KmValueParameter
import kotlin.metadata.Modality
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.createDefaultType

internal abstract class KotlinKCallable<out R> : ReflectKCallableImpl<R>() {
    abstract val modality: Modality
    abstract override val rawBoundReceiver: Any?

    final override val isFinal: Boolean
        get() = modality == Modality.FINAL

    final override val isOpen: Boolean
        get() = modality == Modality.OPEN

    final override val isAbstract: Boolean
        get() = modality == Modality.ABSTRACT

    abstract override val annotations: List<Annotation>
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
        if (!isLocalDelegatedProperty) {
            (container as? KClassImpl<*>)?.let { klass ->
                add(InstanceParameter(callable, klass))
            }
        }
        for (contextParameter in contextParameters) {
            add(KotlinKParameter(callable, contextParameter, size, KParameter.Kind.CONTEXT, typeParameterTable))
        }
        if (receiverParameterType != null) {
            // The name below is only used to create an instance of `KmValueParameter`. It should not leak to the user, because
            // `KotlinKParameter.name` has a check for parameter kind, and returns null for extension receiver parameters.
            val kmParameter = KmValueParameter("<this>").apply { type = receiverParameterType }
            add(KotlinKParameter(callable, kmParameter, size, KParameter.Kind.EXTENSION_RECEIVER, typeParameterTable))
        }
    }
    for (valueParameter in valueParameters) {
        add(KotlinKParameter(callable, valueParameter, size, KParameter.Kind.VALUE, typeParameterTable))
    }
}

private class InstanceParameter(override val callable: KotlinKCallable<*>, klass: KClassImpl<*>) : ReflectKParameter() {
    override val index: Int get() = 0
    override val type: KType = klass.createDefaultType()
    override val name: String? get() = null
    override val kind: KParameter.Kind get() = KParameter.Kind.INSTANCE
    override val isOptional: Boolean get() = false
    override val isVararg: Boolean get() = false
    override val annotations: List<Annotation> get() = emptyList()
    override val declaresDefaultValue: Boolean get() = false
}
