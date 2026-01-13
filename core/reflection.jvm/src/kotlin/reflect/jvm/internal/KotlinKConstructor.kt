/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.metadata.*
import kotlin.metadata.jvm.JvmMethodSignature
import kotlin.metadata.jvm.signature
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.full.createDefaultType

internal class KotlinKConstructor(
    container: KDeclarationContainerImpl,
    signature: String,
    rawBoundReceiver: Any?,
    private val kmConstructor: KmConstructor,
) : KotlinKFunction(container, signature, rawBoundReceiver, KCallableOverriddenStorage.EMPTY) {
    override val contextParameters: List<KmValueParameter> get() = emptyList()
    override val extensionReceiverType: KmType? get() = null
    override val valueParameters: List<KmValueParameter> get() = kmConstructor.valueParameters
    override val typeParameterTable: TypeParameterTable get() = (container as KClassImpl<*>).typeParameterTable
    override val jvmSignature: JvmMethodSignature
        get() = kmConstructor.signature ?: throw KotlinReflectionInternalError("No signature for constructor: $this")

    override val name: String
        get() = "<init>"

    override val returnType: KType by lazy(PUBLICATION) {
        (container as KClassImpl<*>).createDefaultType()
    }

    override val visibility: KVisibility? get() = kmConstructor.visibility.toKVisibility()
    override val modality: Modality get() = Modality.FINAL
    override val isSuspend: Boolean get() = false
    override val isInline: Boolean get() = false
    override val isExternal: Boolean get() = false
    override val isOperator: Boolean get() = false
    override val isInfix: Boolean get() = false

    override val isPrimaryConstructor: Boolean get() = !kmConstructor.isSecondary

    override fun shallowCopy(container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage): ReflectKCallable<Any?> =
        error("Constructors cannot be copied: $this")
}
