/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization.constant

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.constant.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.itOrExpectHasDefaultParameterValue
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf

inline fun <reified T : ConeKotlinType> AnnotationValue.coneTypeSafe(): T? {
    return this.value.type as? T
}

inline fun <reified T : ConeKotlinType> KClassValue.Value.LocalClass.coneType(): T {
    return this.type as T
}

internal fun create(argumentType: ConeKotlinType): ConstantValue<*>? {
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
