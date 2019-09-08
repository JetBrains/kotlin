/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

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
                        when (it) {
                            ConeStarProjection -> "*"
                            is ConeKotlinTypeProjectionIn -> "in ${it.type.render()}"
                            is ConeKotlinTypeProjectionOut -> "out ${it.type.render()}"
                            is ConeKotlinType -> it.render()
                        }
                    })
                }
            }
        }
        is ConeTypeParameterType -> {
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
    } + nullabilitySuffix
}
