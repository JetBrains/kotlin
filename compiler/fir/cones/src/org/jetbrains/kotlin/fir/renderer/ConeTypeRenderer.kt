/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.fir.types.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

open class ConeTypeRenderer() {

    lateinit var builder: StringBuilder
    lateinit var idRenderer: ConeIdRenderer

    open fun renderAsPossibleFunctionType(
        type: ConeKotlinType, renderType: ConeTypeProjection.() -> Unit = { render() }
    ) {
        val kind = type.functionTypeKind
        if (!kind.withPrettyRender()) {
            type.renderType()
            return
        }

        if (type.isMarkedNullable) {
            builder.append("(")
        }
        if (kind == FunctionClassKind.SuspendFunction) {
            builder.append("suspend ")
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

    @OptIn(ExperimentalContracts::class)
    private fun FunctionClassKind?.withPrettyRender(): Boolean {
        contract {
            returns(true) implies (this@withPrettyRender != null)
        }
        return this != null && this != FunctionClassKind.KSuspendFunction && this != FunctionClassKind.KFunction
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

            is ConeIntegerLiteralConstantType -> {
                builder.append("ILT: ${type.value}")
            }

            is ConeIntegerConstantOperatorType -> {
                builder.append("IOT")
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
        builder.append(attributes.joinToString(" ", postfix = " ") { it.toString() })
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
}