/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.types.*

fun ConeKotlinType.renderForDebugInfo(): String {
    val nullabilitySuffix = if (this !is ConeKotlinErrorType && this !is ConeClassErrorType) nullability.suffix else ""
    return when (this) {
        is ConeTypeVariableType -> "TypeVariable(${this.lookupTag.name})"
        is ConeDefinitelyNotNullType -> "${original.renderForDebugInfo()}!!"
        is ConeClassErrorType -> "ERROR CLASS: ${diagnostic.reason}"
        is ConeCapturedType -> "CapturedType(${constructor.projection.renderForDebugInfo()})"
        is ConeClassLikeType -> {
            buildString {
                append(lookupTag.classId.asSingleFqName().asString())
                if (typeArguments.isNotEmpty()) {
                    append(typeArguments.joinToString(prefix = "<", postfix = ">") {
                        it.renderForDebugInfo()
                    })
                }
            }
        }
        is ConeLookupTagBasedType -> {
            lookupTag.name.asString()
        }
        is ConeFlexibleType -> {
            buildString {
                append(lowerBound.renderForDebugInfo())
                append("..")
                append(upperBound.renderForDebugInfo())
            }
        }
        is ConeIntersectionType -> {
            intersectedTypes.joinToString(
                separator = " & ",
            ) { it.renderForDebugInfo() }
        }
        is ConeStubType -> "Stub: $variable"
        is ConeIntegerLiteralType -> "ILT: $value"
    } + nullabilitySuffix
}

private fun ConeTypeProjection.renderForDebugInfo(): String {
    return when (this) {
        ConeStarProjection -> "*"
        is ConeKotlinTypeProjectionIn -> "in ${type.renderForDebugInfo()}"
        is ConeKotlinTypeProjectionOut -> "out ${type.renderForDebugInfo()}"
        is ConeKotlinTypeConflictingProjection -> "CONFLICTING-PROJECTION ${type.renderForDebugInfo()}"
        is ConeKotlinType -> renderForDebugInfo()
    }
}
