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

internal class KotlinKNamedFunction(
    container: KDeclarationContainerImpl,
    signature: String,
    rawBoundReceiver: Any?,
    private val kmFunction: KmFunction,
    overriddenStorage: KCallableOverriddenStorage,
) : KotlinKFunction(container, signature, rawBoundReceiver, overriddenStorage) {
    override val contextParameters: List<KmValueParameter> get() = kmFunction.contextParameters
    override val extensionReceiverType: KmType? get() = kmFunction.receiverParameterType
    override val valueParameters: List<KmValueParameter> get() = kmFunction.valueParameters
    override val typeParameterTable: TypeParameterTable get() = _typeParameterTable.value
    override val jvmSignature: JvmMethodSignature
        get() = kmFunction.signature ?: throw KotlinReflectionInternalError("No signature for function: $this")

    private val _typeParameterTable: Lazy<TypeParameterTable> = lazy(PUBLICATION) {
        val parent = (container as? KClassImpl<*>)?.typeParameterTable
        TypeParameterTable.create(kmFunction.typeParameters, parent, this, container.jClass.classLoader)
    }

    override val name: String
        get() = kmFunction.name

    override val returnType: KType by lazy(PUBLICATION) {
        kmFunction.returnType.toKType(container.jClass.classLoader, typeParameterTable) {
            extractContinuationArgument() ?: caller.returnType
        }
    }

    override val visibility: KVisibility? get() = kmFunction.visibility.toKVisibility()
    override val modality: Modality get() = kmFunction.modality
    override val isSuspend: Boolean get() = kmFunction.isSuspend
    override val isInline: Boolean get() = kmFunction.isInline
    override val isExternal: Boolean get() = kmFunction.isExternal
    override val isOperator: Boolean get() = kmFunction.isOperator
    override val isInfix: Boolean get() = kmFunction.isInfix

    override val isPrimaryConstructor: Boolean get() = false

    override fun replaceContainerForFakeOverride(
        container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage,
    ): ReflectKCallable<Any?> =
        KotlinKNamedFunction(container, signature, rawBoundReceiver, kmFunction, overriddenStorage)
}
