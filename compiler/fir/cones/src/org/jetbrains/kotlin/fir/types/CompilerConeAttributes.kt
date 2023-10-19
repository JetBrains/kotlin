/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import kotlin.reflect.KClass

object CompilerConeAttributes {
    object Exact : ConeAttribute<Exact>() {
        val ANNOTATION_CLASS_ID = ClassId(FqName("kotlin.internal"), Name.identifier("Exact"))

        override fun union(other: Exact?): Exact? = null
        override fun intersect(other: Exact?): Exact? = null
        override fun add(other: Exact?): Exact = this

        override fun isSubtypeOf(other: Exact?): Boolean = true

        override val key: KClass<out Exact> = Exact::class
        override val keepInInferredDeclarationType: Boolean get() = false

        override fun toString(): String = "@Exact"
    }

    object NoInfer : ConeAttribute<NoInfer>() {
        val ANNOTATION_CLASS_ID = ClassId(FqName("kotlin.internal"), Name.identifier("NoInfer"))

        override fun union(other: NoInfer?): NoInfer? = null
        override fun intersect(other: NoInfer?): NoInfer? = null
        override fun add(other: NoInfer?): NoInfer = this
        override fun isSubtypeOf(other: NoInfer?): Boolean = true

        override val key: KClass<out NoInfer> = NoInfer::class
        override val keepInInferredDeclarationType: Boolean get() = false

        override fun toString(): String = "@NoInfer"
    }

    object EnhancedNullability : ConeAttribute<EnhancedNullability>() {
        val ANNOTATION_CLASS_ID = StandardClassIds.Annotations.EnhancedNullability

        override fun union(other: EnhancedNullability?): EnhancedNullability? = other
        override fun intersect(other: EnhancedNullability?): EnhancedNullability = this
        override fun add(other: EnhancedNullability?): EnhancedNullability = this

        override fun isSubtypeOf(other: EnhancedNullability?): Boolean = true

        override val key: KClass<out EnhancedNullability> = EnhancedNullability::class
        override val keepInInferredDeclarationType: Boolean get() = true

        override fun toString(): String = "@EnhancedNullability"
    }

    object ExtensionFunctionType : ConeAttribute<ExtensionFunctionType>() {
        val ANNOTATION_CLASS_ID = ClassId(FqName("kotlin"), Name.identifier("ExtensionFunctionType"))

        override fun union(other: ExtensionFunctionType?): ExtensionFunctionType? = other
        override fun intersect(other: ExtensionFunctionType?): ExtensionFunctionType = this
        override fun add(other: ExtensionFunctionType?): ExtensionFunctionType = this

        override fun isSubtypeOf(other: ExtensionFunctionType?): Boolean = true

        override val key: KClass<out ExtensionFunctionType> = ExtensionFunctionType::class
        override val keepInInferredDeclarationType: Boolean get() = true

        override fun toString(): String = "@ExtensionFunctionType"
    }

    object RawType : ConeAttribute<RawType>() {
        override fun union(other: RawType?): RawType? = other
        override fun intersect(other: RawType?): RawType? = other
        override fun add(other: RawType?): RawType = this
        override fun isSubtypeOf(other: RawType?): Boolean = true

        override val key: KClass<out RawType> = RawType::class
        override val keepInInferredDeclarationType: Boolean get() = true

        override fun toString(): String = "Raw type"
    }

    class ContextFunctionTypeParams(val contextReceiverNumber: Int) : ConeAttribute<ContextFunctionTypeParams>() {
        override fun union(other: ContextFunctionTypeParams?): ContextFunctionTypeParams? = other
        override fun intersect(other: ContextFunctionTypeParams?): ContextFunctionTypeParams = this
        override fun add(other: ContextFunctionTypeParams?): ContextFunctionTypeParams = this

        override fun isSubtypeOf(other: ContextFunctionTypeParams?): Boolean = true

        override val key: KClass<out ContextFunctionTypeParams> = ContextFunctionTypeParams::class
        override val keepInInferredDeclarationType: Boolean get() = true

        override fun toString(): String = "@${StandardNames.FqNames.contextFunctionTypeParams.shortName().asString()}"

        companion object {
            val ANNOTATION_CLASS_ID = ClassId.topLevel(StandardNames.FqNames.contextFunctionTypeParams)
        }
    }

    object UnsafeVariance : ConeAttribute<UnsafeVariance>() {
        val ANNOTATION_CLASS_ID = ClassId(FqName("kotlin"), Name.identifier("UnsafeVariance"))

        override fun union(other: UnsafeVariance?): UnsafeVariance? = null
        override fun intersect(other: UnsafeVariance?): UnsafeVariance? = null
        override fun add(other: UnsafeVariance?): UnsafeVariance = this

        override fun isSubtypeOf(other: UnsafeVariance?): Boolean = true

        override val key: KClass<out UnsafeVariance> = UnsafeVariance::class
        override val keepInInferredDeclarationType: Boolean get() = false

        override fun toString(): String = "@UnsafeVariance"
    }

    private val compilerAttributeByClassId: Map<ClassId, ConeAttributeKey> = mapOf(
        Exact.ANNOTATION_CLASS_ID to Exact.key,
        NoInfer.ANNOTATION_CLASS_ID to NoInfer.key,
        EnhancedNullability.ANNOTATION_CLASS_ID to EnhancedNullability.key,
        ExtensionFunctionType.ANNOTATION_CLASS_ID to ExtensionFunctionType.key,
        UnsafeVariance.ANNOTATION_CLASS_ID to UnsafeVariance.key
    )

    val classIdByCompilerAttributeKey: Map<ConeAttributeKey, ClassId> = compilerAttributeByClassId.entries.associateBy(
        keySelector = { it.value },
        valueTransform = { it.key }
    )

    val compilerAttributeKeyByFqName: Map<FqName, ConeAttributeKey> =
        compilerAttributeByClassId.mapKeys { it.key.asSingleFqName() }
}

val ConeAttributes.exact: CompilerConeAttributes.Exact? by ConeAttributes.attributeAccessor<CompilerConeAttributes.Exact>()
val ConeAttributes.noInfer: CompilerConeAttributes.NoInfer? by ConeAttributes.attributeAccessor<CompilerConeAttributes.NoInfer>()
val ConeAttributes.enhancedNullability: CompilerConeAttributes.EnhancedNullability? by ConeAttributes.attributeAccessor<CompilerConeAttributes.EnhancedNullability>()
val ConeAttributes.extensionFunctionType: CompilerConeAttributes.ExtensionFunctionType? by ConeAttributes.attributeAccessor<CompilerConeAttributes.ExtensionFunctionType>()
private val ConeAttributes.contextFunctionTypeParams: CompilerConeAttributes.ContextFunctionTypeParams? by ConeAttributes.attributeAccessor<CompilerConeAttributes.ContextFunctionTypeParams>()

// ------------------------------------------------------------------

val ConeKotlinType.hasEnhancedNullability: Boolean
    get() = attributes.enhancedNullability != null

val ConeKotlinType.isExtensionFunctionType: Boolean
    get() = attributes.extensionFunctionType != null

val ConeKotlinType.hasContextReceivers: Boolean
    get() = attributes.contextReceiversNumberForFunctionType > 0

val ConeKotlinType.contextReceiversNumberForFunctionType: Int
    get() = attributes.contextReceiversNumberForFunctionType

val ConeAttributes.contextReceiversNumberForFunctionType: Int get() = contextFunctionTypeParams?.contextReceiverNumber ?: 0
