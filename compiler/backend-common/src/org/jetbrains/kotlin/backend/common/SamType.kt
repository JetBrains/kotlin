/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.sam.getAbstractMembers
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.intersectWrappedTypes
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.replaceArgumentsWithNothing

class SamType constructor(val type: KotlinType, val hasUnapproximatableArguments: Boolean = false) {

    val classDescriptor: ClassDescriptor
        get() = type.constructor.declarationDescriptor as? ClassDescriptor ?: error("Sam/Fun interface not a class descriptor: $type")

    val kotlinFunctionType: KotlinType
        get() = classDescriptor.defaultFunctionTypeForSamInterface!!

    val originalAbstractMethod: SimpleFunctionDescriptor
        get() = getAbstractMembers(classDescriptor)[0] as SimpleFunctionDescriptor

    override fun equals(other: Any?): Boolean {
        return other is SamType && type == other.type
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }

    override fun toString(): String {
        return "SamType($type)"
    }
}

open class SamTypeFactory {
    private lateinit var typeApproximator: TypeApproximator

    fun createByValueParameter(valueParameter: ValueParameterDescriptor, languageVersionSettings: LanguageVersionSettings): SamType? {
        val singleArgumentType: KotlinType
        val originalSingleArgumentType: KotlinType?
        val varargElementType = valueParameter.varargElementType
        if (varargElementType != null) {
            singleArgumentType = varargElementType
            originalSingleArgumentType = valueParameter.original.varargElementType
            assert(originalSingleArgumentType != null) {
                "Value parameter and original value parameter have inconsistent varargs: " +
                        valueParameter + "; " + valueParameter.original
            }
        } else {
            singleArgumentType = valueParameter.type
            originalSingleArgumentType = valueParameter.original.type
        }
        if (singleArgumentType.isError || originalSingleArgumentType!!.isError) {
            return null
        }

        if (!::typeApproximator.isInitialized) {
            typeApproximator = TypeApproximator(singleArgumentType.builtIns, languageVersionSettings)
        }

        // This can be true in case when the value parameter is in the method of a generic type with out-projection.
        // We approximate Inv<Captured#1> to Nothing, while Inv itself can be a SAM interface safe to call here
        // (see testData genericSamProjectedOut.kt for details)
        // In such a case we can't have a proper supertype since wildcards are not allowed there,
        // so we use Nothing arguments instead that leads to a raw type used for a SAM wrapper.
        // See org.jetbrains.kotlin.codegen.state.KotlinTypeMapper#writeGenericType to understand how
        // raw types and Nothing arguments relate.
        val originalTypeToUse = if (KotlinBuiltIns.isNothing(singleArgumentType))
            originalSingleArgumentType.replaceArgumentsWithNothing()
        else singleArgumentType
        val approximatedOriginalTypeToUse = typeApproximator.approximateToSubType(
            originalTypeToUse,
            TypeApproximatorConfiguration.UpperBoundAwareIntersectionTypeApproximator
        ) ?: originalTypeToUse
        approximatedOriginalTypeToUse as KotlinType
        val hasUnapproximatableArguments = hasUnapproximatableArguments(approximatedOriginalTypeToUse)

        return create((approximatedOriginalTypeToUse).removeExternalProjections(), hasUnapproximatableArguments)
    }

    /*
     * declaration site: `class Foo<T> where T: X, T: Y {}`, X and Y aren't related by subtyping
     * use site: Foo<in {X & Y}>
     * => `in {X & Y}` is unapproximatable type argument
     */
    fun hasUnapproximatableArguments(type: KotlinType) =
        type.arguments.isNotEmpty() && type.arguments.withIndex().any { (i, argument) ->
            if (argument.projectionKind != Variance.IN_VARIANCE) return@any false
            if (argument.type.constructor !is IntersectionTypeConstructor) return@any false
            val typeParameter = type.constructor.parameters.getOrNull(i) ?: return@any false
            // we have really intersection type as the result => at least there are two upper bounds which aren't related by subtyping
            intersectWrappedTypes(typeParameter.upperBounds).constructor is IntersectionTypeConstructor
        }

    open fun isSamType(type: KotlinType): Boolean {
        val descriptor = type.constructor.declarationDescriptor
        return descriptor is ClassDescriptor && descriptor.isFun
    }

    @JvmOverloads
    fun create(originalType: KotlinType, hasUnapproximatableArguments: Boolean = false): SamType? {
        return if (isSamType(originalType)) SamType(originalType, hasUnapproximatableArguments) else null
    }

    private fun KotlinType.removeExternalProjections(): KotlinType {
        val newArguments = arguments.map { TypeProjectionImpl(Variance.INVARIANT, it.type) }
        return replace(newArguments)
    }

    companion object {
        val INSTANCE = SamTypeFactory()
    }
}
