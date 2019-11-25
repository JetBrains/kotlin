/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun ConeKotlinType.render(): String {
    val nullabilitySuffix = if (this !is ConeKotlinErrorType && this !is ConeClassErrorType) nullability.suffix else ""
    return when (this) {
        is ConeTypeVariableType -> "TypeVariable(${this.lookupTag.name})"
        is ConeDefinitelyNotNullType -> "${original.render()}!"
        is ConeClassErrorType -> "class error: $reason"
        is ConeCapturedType -> "captured type: lowerType = ${lowerType?.render()}"
        is ConeClassLikeType -> {
            buildString {
                append(lookupTag.classId.asString())
                if (typeArguments.isNotEmpty()) {
                    append(typeArguments.joinToString(prefix = "<", postfix = ">") {
                        it.render()
                    })
                }
            }
        }
        is ConeLookupTagBasedType -> {
            lookupTag.name.asString()
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
                prefix = "it(",
                postfix = ")"
            )
        }
        is ConeStubType -> "stub type: $variable"
    } + nullabilitySuffix
}

private fun ConeKotlinTypeProjection.render(): String {
    return when (this) {
        ConeStarProjection -> "*"
        is ConeKotlinTypeProjectionIn -> "in ${type.render()}"
        is ConeKotlinTypeProjectionOut -> "out ${type.render()}"
        is ConeKotlinType -> render()
    }
}

fun ConeKotlinType.renderFunctionType(kind: FunctionClassDescriptor.Kind?, isExtension: Boolean): String {
    if (!kind.withPrettyRender()) return render()
    return buildString {
        if (kind == FunctionClassDescriptor.Kind.SuspendFunction) {
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
            append(receiver.render())
            append(".")
        }
        append(arguments.joinToString(", ", "(", ")") { it.render() })
        append(" -> ")
        append(returnType.render())
    }
}

@UseExperimental(ExperimentalContracts::class)
fun FunctionClassDescriptor.Kind?.withPrettyRender(): Boolean {
    contract {
        returns(true) implies (this@withPrettyRender != null)
    }
    return this != null && this != FunctionClassDescriptor.Kind.KSuspendFunction && this != FunctionClassDescriptor.Kind.KFunction
}