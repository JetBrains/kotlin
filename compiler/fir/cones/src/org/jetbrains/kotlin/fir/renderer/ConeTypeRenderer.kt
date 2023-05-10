/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.types.*

open class ConeTypeRenderer {

    lateinit var builder: StringBuilder
    lateinit var idRenderer: ConeIdRenderer
    var attributeRenderer: ConeAttributeRenderer = ConeAttributeRenderer.ToString

    open fun renderAsPossibleFunctionType(
        type: ConeKotlinType,
        functionClassKindExtractor: (ConeKotlinType) -> FunctionTypeKind?,
        renderType: ConeTypeProjection.() -> Unit = { render() }
    ) {
        val kind = functionClassKindExtractor(type)
        if (kind?.isReflectType != false) {
            type.renderType()
            return
        }

        type.renderNonCompilerAttributes()

        if (type.isMarkedNullable) {
            builder.append("(")
        }
        kind.prefixForTypeRender?.let {
            builder.append(it)
            builder.append(" ")
        }
        val typeArguments = type.typeArguments
        val isExtension = type.isExtensionFunctionType
        val (receiver, otherTypeArguments) = if (isExtension && typeArguments.first() != ConeStarProjection) {
            typeArguments.first() to typeArguments.drop(1)
        } else {
            null to typeArguments.toList()
        }
        val arguments = otherTypeArguments.subList(0, otherTypeArguments.size - 1)
        val returnType = otherTypeArguments.last()
        if (receiver != null) {
            receiver.render()
            builder.append(".")
        }
        builder.append("(")
        for ((index, argument) in arguments.withIndex()) {
            if (index != 0) {
                builder.append(", ")
            }
            argument.render()
        }
        builder.append(") -> ")
        returnType.render()
        if (type.isMarkedNullable) {
            builder.append(")?")
        }
    }

    fun render(type: ConeKotlinType) {
        if (type !is ConeFlexibleType && type !is ConeDefinitelyNotNullType) {
            // We don't render attributes for flexible/definitely not null types here,
            // because bounds duplicate these attributes often
            type.renderAttributes()
        }
        when (type) {
            is ConeTypeVariableType -> {
                builder.append("TypeVariable(")
                builder.append(type.lookupTag.name)
                builder.append(")")
            }

            is ConeDefinitelyNotNullType -> {
                render(type.original)
                builder.append(" & Any")
            }

            is ConeErrorType -> {
                builder.append("ERROR CLASS: ${type.diagnostic.reason}")
            }

            is ConeCapturedType -> {
                builder.append("CapturedType(")
                type.constructor.projection.render()
                builder.append(")")
            }

            is ConeClassLikeType -> {
                type.render()
            }

            is ConeLookupTagBasedType -> {
                builder.append(type.lookupTag.name.asString())
            }

            is ConeDynamicType -> {
                builder.append("dynamic")
            }

            is ConeFlexibleType -> {
                render(type)
            }

            is ConeIntersectionType -> {
                builder.append("it(")
                for ((index, intersected) in type.intersectedTypes.withIndex()) {
                    if (index > 0) {
                        builder.append(" & ")
                    }
                    render(intersected)
                }
                builder.append(")")
            }

            is ConeStubTypeForSyntheticFixation -> {
                builder.append("Stub (fixation): ${type.constructor.variable}")
            }

            is ConeStubTypeForChainInference -> {
                builder.append("Stub (chain inference): ${type.constructor.variable}")
            }

            is ConeStubType -> {
                builder.append("Stub (subtyping): ${type.constructor.variable}")
            }

            is ConeIntegerLiteralType -> {
                render(type)
            }
        }
        if (type !is ConeFlexibleType && type !is ConeErrorType) {
            builder.append(type.nullability.suffix)
        }
    }

    private fun ConeClassLikeType.render() {
        idRenderer.renderClassId(lookupTag.classId)
        if (typeArguments.isEmpty()) return
        builder.append("<")
        for ((index, typeArgument) in typeArguments.withIndex()) {
            if (index > 0) {
                builder.append(", ")
            }
            typeArgument.render()
        }
        builder.append(">")
    }

    private fun ConeFlexibleType.renderForSameLookupTags(): Boolean {
        if (lowerBound is ConeLookupTagBasedType && upperBound is ConeLookupTagBasedType &&
            lowerBound.lookupTag == upperBound.lookupTag &&
            lowerBound.nullability == ConeNullability.NOT_NULL && upperBound.nullability == ConeNullability.NULLABLE
        ) {
            if (lowerBound !is ConeClassLikeType || lowerBound.typeArguments.isEmpty()) {
                if (upperBound !is ConeClassLikeType || upperBound.typeArguments.isEmpty()) {
                    render(lowerBound)
                    builder.append("!")
                    return true
                }
            }
        }
        return false
    }

    protected open fun render(flexibleType: ConeFlexibleType) {
        if (flexibleType.renderForSameLookupTags()) {
            return
        }
        builder.append("ft<")
        render(flexibleType.lowerBound)
        builder.append(", ")
        render(flexibleType.upperBound)
        builder.append(">")
    }

    private fun ConeKotlinType.renderAttributes() {
        if (!attributes.any()) return
        builder.append(attributeRenderer.render(attributes))
    }

    private fun ConeKotlinType.renderNonCompilerAttributes() {
        val compilerAttributes = CompilerConeAttributes.classIdByCompilerAttributeKey
        if (attributes.any { it.key !in compilerAttributes }) {
            builder.append(attributeRenderer.render(attributes))
        }
    }

    private fun ConeTypeProjection.render() {
        when (this) {
            ConeStarProjection -> {
                builder.append("*")
            }

            is ConeKotlinTypeConflictingProjection -> {
                builder.append("CONFLICTING-PROJECTION ")
                render(type)
            }

            is ConeKotlinTypeProjectionIn -> {
                builder.append("in ")
                render(type)
            }

            is ConeKotlinTypeProjectionOut -> {
                builder.append("out ")
                render(type)
            }

            is ConeKotlinType -> {
                render(this)
            }
        }
    }

    protected open fun render(type: ConeIntegerLiteralType) {
        when (type) {
            is ConeIntegerLiteralConstantType -> {
                builder.append("ILT: ${type.value}")
            }

            is ConeIntegerConstantOperatorType -> {
                builder.append("IOT")
            }
        }
    }
}
