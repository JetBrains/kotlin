/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.runtime.structure.safeClassLoader
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.jvm.internal.CallableReference
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
        // In JVM metadata, functions always have `signature`. In builtins metadata, they don't, so we compute it manually from the
        // `signature` parameter that comes from the function reference.
        get() = kmFunction.signature ?: convertSignatureForBuiltinFunction(signature)
    override val metadataAnnotations: List<KmAnnotation> get() = kmFunction.annotations

    private val _typeParameterTable: Lazy<TypeParameterTable> = lazy(PUBLICATION) {
        val parent = ((overriddenStorage.originalContainerIfFakeOverride ?: container) as? KClassImpl<*>)?.typeParameterTable
        TypeParameterTable.create(kmFunction.typeParameters, parent, this, container.jClass.safeClassLoader)
    }

    override val name: String
        get() = kmFunction.name

    override val returnType: KType by lazy(PUBLICATION) {
        substituteType(kmFunction.returnType.toKType(container.jClass.safeClassLoader, typeParameterTable) {
            extractContinuationArgument() ?: caller.returnType
        })
    }

    override val visibility: KVisibility? get() = kmFunction.visibility.toKVisibility()
    override val modality: Modality get() = overriddenStorage.modality ?: kmFunction.modality
    override val isSuspend: Boolean get() = kmFunction.isSuspend
    override val isInline: Boolean get() = overriddenStorage.forceIsInline || kmFunction.isInline
    override val isExternal: Boolean get() = overriddenStorage.forceIsExternal || kmFunction.isExternal
    override val isOperator: Boolean get() = overriddenStorage.forceIsOperator || kmFunction.isOperator
    override val isInfix: Boolean get() = overriddenStorage.forceIsInfix || kmFunction.isInfix

    override val isPrimaryConstructor: Boolean get() = false

    @OptIn(ExperimentalCompanionBlocksAndExtensions::class)
    val isCompanionBlockMember: Boolean
        get() = container is KClassImpl<*> && kmFunction.isStatic

    override val overridden: Collection<ReflectKFunction> by lazy(PUBLICATION) {
        computeOverriddenFunctions(this)
    }

    override fun shallowCopy(container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage): ReflectKCallable<Any?> =
        KotlinKNamedFunction(container, signature, CallableReference.NO_RECEIVER, kmFunction, overriddenStorage)

    override fun rebind(boundReceiver: Any?): ReflectKCallable<Any?> =
        if (this.rawBoundReceiver === boundReceiver) this
        else KotlinKNamedFunction(container, signature, boundReceiver, kmFunction, overriddenStorage)

    private fun convertSignatureForBuiltinFunction(signature: String): JvmMethodSignature =
        with(signature) {
            val paren = indexOfLast { it == '(' }
            JvmMethodSignature(substring(0, paren), substring(paren))
        }
}
