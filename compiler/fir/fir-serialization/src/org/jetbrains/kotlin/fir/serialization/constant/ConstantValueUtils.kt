/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization.constant

import org.jetbrains.kotlin.constant.KClassValue
import org.jetbrains.kotlin.fir.types.*

internal fun create(argumentType: ConeKotlinType): KClassValue? {
    if (argumentType is ConeErrorType) return null
    if (argumentType !is ConeClassLikeType) return null
    var type = argumentType
    var arrayDimensions = 0
    while (true) {
        if (type.isPrimitiveArray) break
        type = type.arrayElementType() ?: break
        arrayDimensions++
    }
    val classId = type.classId ?: return null
    return KClassValue(classId, arrayDimensions)
}
