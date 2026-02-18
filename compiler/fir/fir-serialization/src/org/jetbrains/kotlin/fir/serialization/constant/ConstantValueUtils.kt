/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization.constant

import org.jetbrains.kotlin.constant.KClassValue
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toClassLikeSymbol
import org.jetbrains.kotlin.fir.types.*

internal fun create(argumentType: ConeKotlinType, session: FirSession): KClassValue? {
    if (argumentType is ConeErrorType) return null
    if (argumentType !is ConeClassLikeType) return null
    var type: ConeKotlinType = argumentType.fullyExpandedType(session)
    var arrayDimensions = 0
    while (true) {
        if (type.isPrimitiveArray) break
        type = type.arrayElementType() ?: break
        arrayDimensions++
    }
    val classSymbol = type.toClassLikeSymbol(session) ?: return null
    return when {
        classSymbol.isLocal -> KClassValue(KClassValue.Value.LocalClass(firClassSymbol = classSymbol))
        else -> KClassValue(classSymbol.classId, arrayDimensions)
    }
}
