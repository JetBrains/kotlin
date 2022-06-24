/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun ConeKotlinType.render(renderFqNames: Boolean = true): String {
    val nullabilitySuffix = if (this !is ConeFlexibleType && this !is ConeErrorType) nullability.suffix else ""
    return when (this) {
        is ConeTypeVariableType -> "${renderAttributes()}TypeVariable(${this.lookupTag.name})"
        is ConeDefinitelyNotNullType -> "${original.render(renderFqNames)} & Any"
        is ConeErrorType -> "${renderAttributes()}ERROR CLASS: ${diagnostic.reason}"
        is ConeCapturedType -> "${renderAttributes()}CapturedType(${constructor.projection.render(renderFqNames)})"
        is ConeClassLikeType -> {
            buildString {
                append(renderAttributes())
                if (renderFqNames) {
                    append(lookupTag.classId.asString())
                } else {
                    append(lookupTag.classId.relativeClassName.asString())
                }
                if (typeArguments.isNotEmpty()) {
                    append(typeArguments.joinToString(prefix = "<", postfix = ">") {
                        it.render(renderFqNames)
                    })
                }
            }
        }
        is ConeLookupTagBasedType -> {
            "${renderAttributes()}${lookupTag.name.asString()}"
        }
        is ConeDynamicType -> "dynamic"
        is ConeFlexibleType -> this.render(renderFqNames)
        is ConeIntersectionType -> {
            intersectedTypes.joinToString(
                separator = " & ",
                prefix = "${renderAttributes()}it(",
                postfix = ")"
            ) {
                it.render(renderFqNames)
            }
        }
        is ConeStubTypeForSyntheticFixation -> "${renderAttributes()}Stub (fixation): ${constructor.variable}"
        is ConeStubTypeForChainInference -> "${renderAttributes()}Stub (chain inference): ${constructor.variable}"
        is ConeStubType -> "${renderAttributes()}Stub (subtyping): ${constructor.variable}"
        is ConeIntegerLiteralConstantType -> "${renderAttributes()}ILT: $value"
        is ConeIntegerConstantOperatorType -> "${renderAttributes()}IOT"
    } + nullabilitySuffix
}

private fun ConeFlexibleType.render(renderFqNames: Boolean): String {
    if (lowerBound is ConeLookupTagBasedType && upperBound is ConeLookupTagBasedType &&
        lowerBound.lookupTag == upperBound.lookupTag &&
        lowerBound.nullability == ConeNullability.NOT_NULL && upperBound.nullability == ConeNullability.NULLABLE
    ) {
        if (lowerBound !is ConeClassLikeType || lowerBound.typeArguments.isEmpty()) {
            if (upperBound !is ConeClassLikeType || upperBound.typeArguments.isEmpty()) {
                return buildString {
                    append(lowerBound.render(renderFqNames))
                    append("!")
                }
            }
        }
    }
    return buildString {
        append("ft<")
        append(lowerBound.render(renderFqNames))
        append(", ")
        append(upperBound.render(renderFqNames))
        append(">")
    }
}

private fun ConeKotlinType.renderAttributes(): String {
    if (!attributes.any()) return ""
    return attributes.joinToString(" ", postfix = " ") { it.toString() }
}

fun ConeTypeProjection.render(renderFqNames: Boolean): String {
    return when (this) {
        ConeStarProjection -> "*"
        is ConeKotlinTypeConflictingProjection -> "CONFLICTING-PROJECTION ${type.render(renderFqNames)}"
        is ConeKotlinTypeProjectionIn -> "in ${type.render(renderFqNames)}"
        is ConeKotlinTypeProjectionOut -> "out ${type.render(renderFqNames)}"
        is ConeKotlinType -> render(renderFqNames)
    }
}

fun ConeKotlinType.renderFunctionType(
    kind: FunctionClassKind?, renderFqNames: Boolean, renderType: ConeTypeProjection.() -> String = { render(renderFqNames) }
): String {
    if (!kind.withPrettyRender()) return renderType()

    val isExtension = isExtensionFunctionType

    val renderedType = buildString {
        if (kind == FunctionClassKind.SuspendFunction) {
            append("suspend ")
        }
        val (receiver, otherTypeArguments) = if (isExtension && typeArguments.first() != ConeStarProjection) {
            typeArguments.first() to typeArguments.drop(1)
        } else {
            null to typeArguments.toList()
        }
        val arguments = otherTypeArguments.subList(0, otherTypeArguments.size - 1)
        val returnType = otherTypeArguments.last()
        if (receiver != null) {
            append(receiver.renderType())
            append(".")
        }
        append(arguments.joinToString(", ", "(", ")") { it.renderType() })
        append(" -> ")
        append(returnType.renderType())
    }
    return if (isMarkedNullable) "($renderedType)?" else renderedType
}

@OptIn(ExperimentalContracts::class)
fun FunctionClassKind?.withPrettyRender(): Boolean {
    contract {
        returns(true) implies (this@withPrettyRender != null)
    }
    return this != null && this != FunctionClassKind.KSuspendFunction && this != FunctionClassKind.KFunction
}
