/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun ConeKotlinType.render(): String {
    val nullabilitySuffix = if (this !is ConeFlexibleType && this !is ConeErrorType) nullability.suffix else ""
    return when (this) {
        is ConeTypeVariableType -> "${renderAttributes()}TypeVariable(${this.lookupTag.name})"
        is ConeDefinitelyNotNullType -> "${original.render()} & Any"
        is ConeErrorType -> "${renderAttributes()}ERROR CLASS: ${diagnostic.reason}"
        is ConeCapturedType -> "${renderAttributes()}CapturedType(${constructor.projection.render()})"
        is ConeClassLikeType -> {
            buildString {
                append(renderAttributes())
                append(lookupTag.classId.asString())
                if (typeArguments.isNotEmpty()) {
                    append(typeArguments.joinToString(prefix = "<", postfix = ">") {
                        it.render()
                    })
                }
            }
        }
        is ConeLookupTagBasedType -> {
            "${renderAttributes()}${lookupTag.name.asString()}"
        }
        is ConeDynamicType -> {
            buildString {
                append("dynamic")
            }
        }
        is ConeFlexibleType -> {
            buildString {
                append("ft<")
                append(lowerBound.render())
                append(", ")
                append(upperBound.render())
                append(">")
            }
        }
        is ConeIntersectionType -> {
            intersectedTypes.joinToString(
                separator = " & ",
                prefix = "${renderAttributes()}it(",
                postfix = ")"
            )
        }
        is ConeStubTypeForChainInference -> "${renderAttributes()}Stub (chain inference): ${constructor.variable}"
        is ConeStubTypeForSyntheticFixation -> "${renderAttributes()}Stub (fixation): ${constructor.variable}"
        is ConeStubType -> "${renderAttributes()}Stub (subtyping): ${constructor.variable}"
        is ConeIntegerLiteralConstantType -> "${renderAttributes()}ILT: $value"
        is ConeIntegerConstantOperatorType -> "${renderAttributes()}IOT"
    } + nullabilitySuffix
}

private fun ConeKotlinType.renderAttributes(): String {
    if (!attributes.any()) return ""
    return attributes.joinToString(" ", postfix = " ") { it.toString() }
}

fun ConeTypeProjection.render(): String {
    return when (this) {
        ConeStarProjection -> "*"
        is ConeKotlinTypeConflictingProjection -> "CONFLICTING-PROJECTION ${type.render()}"
        is ConeKotlinTypeProjectionIn -> "in ${type.render()}"
        is ConeKotlinTypeProjectionOut -> "out ${type.render()}"
        is ConeKotlinType -> render()
    }
}

fun ConeKotlinType.renderFunctionType(
    kind: FunctionClassKind?, isExtension: Boolean, renderType: ConeTypeProjection.() -> String = { render() }
): String {
    if (!kind.withPrettyRender()) return renderType()
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
