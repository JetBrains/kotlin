/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.reflect.KClass

object CompilerConeAttributes {
    object Exact : ConeAttribute<Exact>() {
        val ANNOTATION_CLASS_ID = ClassId(FqName("kotlin.internal"), Name.identifier("Exact"))

        override fun union(other: Exact?): Exact? = null
        override fun intersect(other: Exact?): Exact? = null
        override fun isSubtypeOf(other: Exact?): Boolean = true

        override val key: KClass<out Exact> = Exact::class

        override fun toString(): String = "@Exact"
    }

    object NoInfer : ConeAttribute<NoInfer>() {
        val ANNOTATION_CLASS_ID = ClassId(FqName("kotlin.internal"), Name.identifier("NoInfer"))

        override fun union(other: NoInfer?): NoInfer? = null
        override fun intersect(other: NoInfer?): NoInfer? = null
        override fun isSubtypeOf(other: NoInfer?): Boolean = true

        override val key: KClass<out NoInfer> = NoInfer::class

        override fun toString(): String = "@NoInfer"
    }

    object EnhancedNullability : ConeAttribute<EnhancedNullability>() {
        val ANNOTATION_CLASS_ID = ClassId(FqName("kotlin.jvm.internal"), Name.identifier("EnhancedNullability"))

        override fun union(other: EnhancedNullability?): EnhancedNullability? = other
        override fun intersect(other: EnhancedNullability?): EnhancedNullability? = this
        override fun isSubtypeOf(other: EnhancedNullability?): Boolean = true

        override val key: KClass<out EnhancedNullability> = EnhancedNullability::class

        override fun toString(): String = "@EnhancedNullability"
    }

    object ExtensionFunctionType : ConeAttribute<ExtensionFunctionType>() {
        val ANNOTATION_CLASS_ID = ClassId(FqName("kotlin"), Name.identifier("ExtensionFunctionType"))

        override fun union(other: ExtensionFunctionType?): ExtensionFunctionType? = other
        override fun intersect(other: ExtensionFunctionType?): ExtensionFunctionType? = this
        override fun isSubtypeOf(other: ExtensionFunctionType?): Boolean = true

        override val key: KClass<out ExtensionFunctionType> = ExtensionFunctionType::class

        override fun toString(): String = "@ExtensionFunctionType"
    }

    object FlexibleNullability : ConeAttribute<FlexibleNullability>() {
        val ANNOTATION_CLASS_ID = ClassId(FqName("kotlin.internal.ir"), Name.identifier("FlexibleNullability"))

        override fun union(other: FlexibleNullability?): FlexibleNullability? = other
        override fun intersect(other: FlexibleNullability?): FlexibleNullability? = this
        override fun isSubtypeOf(other: FlexibleNullability?): Boolean = true

        override val key: KClass<out FlexibleNullability> = FlexibleNullability::class

        override fun toString(): String = "@FlexibleNullability"
    }

    object UnsafeVariance : ConeAttribute<UnsafeVariance>() {
        val ANNOTATION_CLASS_ID = ClassId(FqName("kotlin"), Name.identifier("UnsafeVariance"))

        override fun union(other: UnsafeVariance?): UnsafeVariance? = null
        override fun intersect(other: UnsafeVariance?): UnsafeVariance? = null
        override fun isSubtypeOf(other: UnsafeVariance?): Boolean = true

        override val key: KClass<out UnsafeVariance> = UnsafeVariance::class

        override fun toString(): String = "@UnsafeVariance"
    }
}

val ConeAttributes.exact: CompilerConeAttributes.Exact? by ConeAttributes.attributeAccessor<CompilerConeAttributes.Exact>()
val ConeAttributes.noInfer: CompilerConeAttributes.NoInfer? by ConeAttributes.attributeAccessor<CompilerConeAttributes.NoInfer>()
val ConeAttributes.enhancedNullability: CompilerConeAttributes.EnhancedNullability? by ConeAttributes.attributeAccessor<CompilerConeAttributes.EnhancedNullability>()
val ConeAttributes.extensionFunctionType: CompilerConeAttributes.ExtensionFunctionType? by ConeAttributes.attributeAccessor<CompilerConeAttributes.ExtensionFunctionType>()
val ConeAttributes.flexibleNullability: CompilerConeAttributes.FlexibleNullability? by ConeAttributes.attributeAccessor<CompilerConeAttributes.FlexibleNullability>()
val ConeAttributes.unsafeVarianceType: CompilerConeAttributes.UnsafeVariance? by ConeAttributes.attributeAccessor<CompilerConeAttributes.UnsafeVariance>()

val ConeKotlinType.hasEnhancedNullability: Boolean
    get() = attributes.enhancedNullability != null

val ConeKotlinType.isExtensionFunctionType: Boolean
    get() = attributes.extensionFunctionType != null

val ConeKotlinType.hasFlexibleNullability: Boolean
    get() = attributes.flexibleNullability != null
