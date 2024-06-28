/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.arrayElementType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isArrayType
import org.jetbrains.kotlin.fir.types.isString
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.variance
import org.jetbrains.kotlin.types.Variance

/**
 * Returns true if the receiver is a main-like function with the correct parameters and return type
 * Namely the following variants are considered valid:
 *   - fun main(): Unit - iff top-level
 *   - fun main(args: Array<[out] String>): Unit - including vararg case
 *   - fun Array<[out] String>.main(): Unit - see #KTIJ-7949
 * where "main" name is checked against either platform name or the "main" string literally.
 * In addition, the function is assumed valid if it is a top-level one or is marked as platform static.
 *
 * It is a partial implementation of MainFunctionDetector.isMain over Fir; namely, this implementation doesn't cover all cases supported
 * by the original one, so by itself it may return true for some "incorrect" candidates. Some additional checks, like belonging
 * to a singleton, should be checked externally.
 */
fun FirSimpleFunction.isMaybeMainFunction(
    getPlatformName: FirSimpleFunction.() -> String?,
    isPlatformStatic: FirSimpleFunction.() -> Boolean,
): Boolean {
    if (isLocal) return false
    if (typeParameters.isNotEmpty()) return false
    if (!returnTypeRef.coneType.isUnit) return false

    val isTopLevel = containingClassLookupTag() == null

    if ((getPlatformName() ?: name.asString()) != "main") return false

    val valueParametersTypes = valueParameters.map { it.returnTypeRef } +
            // Support for "extension main", which in fact compiles to a correct (from JVM point of view) main method. See #KTIJ-7949
            listOfNotNull(receiverParameter?.typeRef)

    val hasValidParameters =
        when (valueParametersTypes.size) {
            // Comment copied from MainFunctionDetector.isMain:
            // We do not support parameterless entry points having JvmName("name") but different real names
            // See more at https://github.com/Kotlin/KEEP/blob/master/proposals/enhancing-main-convention.md#parameterless-main
            0 -> isTopLevel && name.asString() == "main"
            1 -> valueParametersTypes.single().coneType.isValidMainFunctionParameter()
            else -> false
        }
    return hasValidParameters && (isTopLevel || isPlatformStatic())
}

private fun ConeKotlinType.isValidMainFunctionParameter() =
    isArrayType &&
            arrayElementType()?.isString == true &&
            this.variance != Variance.IN_VARIANCE
