/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.java.lazy.types

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.error.ErrorTypeKind
import org.jetbrains.kotlin.types.error.ErrorUtils

internal class RawSubstitution(typeParameterUpperBoundEraser: TypeParameterUpperBoundEraser? = null) : TypeSubstitution() {
    private val projectionComputer = RawProjectionComputer()
    private val typeParameterUpperBoundEraser = typeParameterUpperBoundEraser ?: TypeParameterUpperBoundEraser(projectionComputer)

    override fun get(key: KotlinType) = TypeProjectionImpl(eraseType(key))

    private fun eraseType(type: KotlinType, attr: JavaTypeAttributes = JavaTypeAttributes(TypeUsage.COMMON)): KotlinType {
        return when (val declaration = type.constructor.declarationDescriptor) {
            is TypeParameterDescriptor ->
                eraseType(typeParameterUpperBoundEraser.getErasedUpperBound(declaration, attr.markIsRaw(true)), attr)
            is ClassDescriptor -> {
                val declarationForUpper =
                    type.upperIfFlexible().constructor.declarationDescriptor

                check(declarationForUpper is ClassDescriptor) {
                    "For some reason declaration for upper bound is not a class " +
                            "but \"$declarationForUpper\" while for lower it's \"$declaration\""
                }

                val lower = eraseInflexibleBasedOnClassDescriptor(type.lowerIfFlexible(), declaration, lowerTypeAttr)
                val upper = eraseInflexibleBasedOnClassDescriptor(type.upperIfFlexible(), declarationForUpper, upperTypeAttr)

                KotlinTypeFactory.flexibleType(lower, upper)
            }
            else -> error("Unexpected declaration kind: $declaration")
        }
    }

    // false means that type cannot be raw
    private fun eraseInflexibleBasedOnClassDescriptor(
        type: SimpleType, declaration: ClassDescriptor, attr: JavaTypeAttributes
    ): SimpleType {
        if (type.constructor.parameters.isEmpty()) return type

        if (KotlinBuiltIns.isArray(type)) {
            val componentTypeProjection = type.arguments[0]
            val arguments = listOf(
                TypeProjectionImpl(componentTypeProjection.projectionKind, eraseType(componentTypeProjection.type, attr))
            )
            return KotlinTypeFactory.simpleType(
                type.attributes, type.constructor, arguments, type.isMarkedNullable
            )
        }

        if (type.isError) {
            return ErrorUtils.createErrorType(ErrorTypeKind.ERROR_RAW_TYPE, type.constructor.toString())
        }

        val memberScope = declaration.getMemberScope(this)
        return KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
            type.attributes, declaration.typeConstructor,
            declaration.typeConstructor.parameters.map { parameter ->
                projectionComputer.computeProjection(parameter, attr, typeParameterUpperBoundEraser)
            },
            type.isMarkedNullable, memberScope
        ) factory@{ kotlinTypeRefiner ->
            val classId = (declaration as? ClassDescriptor)?.classId ?: return@factory null

            @OptIn(TypeRefinement::class)
            val refinedClassDescriptor = kotlinTypeRefiner.findClassAcrossModuleDependencies(classId) ?: return@factory null
            if (refinedClassDescriptor == declaration) return@factory null

            eraseInflexibleBasedOnClassDescriptor(type, refinedClassDescriptor, attr)
        }
    }

    override fun isEmpty() = false

    companion object {
        private val lowerTypeAttr = TypeUsage.COMMON.toAttributes(isRaw = true).withFlexibility(JavaTypeFlexibility.FLEXIBLE_LOWER_BOUND)
        private val upperTypeAttr = TypeUsage.COMMON.toAttributes(isRaw = true).withFlexibility(JavaTypeFlexibility.FLEXIBLE_UPPER_BOUND)
    }
}
