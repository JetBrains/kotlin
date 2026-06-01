/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.runtime.structure.safeClassLoader
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

    override val extensionReceiverType: KmType? by lazy(PUBLICATION) {
        @OptIn(ExperimentalCompanionBlocksAndExtensions::class)
        kmFunction.receiverParameterType.takeUnless { kmFunction.isStatic }
    }

    override val valueParameters: List<KmValueParameter> get() = kmFunction.valueParameters
    override val typeParameterTable: TypeParameterTable get() = _typeParameterTable.value

    override val jvmSignature: JvmMethodSignature
        // In JVM metadata, functions always have `signature`. In builtins metadata, they don't, so we compute it manually from the
        // `signature` parameter that comes from the function reference.
        get() = kmFunction.signature ?: convertSignatureForBuiltinFunction(signature)

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

    override fun shallowCopy(
        container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage, boundReceiver: Any?,
    ): ReflectKCallable<Any?> =
        KotlinKNamedFunction(container, signature, boundReceiver, kmFunction, overriddenStorage)

    private fun convertSignatureForBuiltinFunction(signature: String): JvmMethodSignature =
        with(signature) {
            // This wouldn't work for functions with '(' in the name (allowed by JVM), but there are no such functions in builtins
            // (and hopefully there won't be any in the future).
            val paren = indexOf('(')
            JvmMethodSignature(substring(0, paren), substring(paren))
        }
}
