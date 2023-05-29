/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization.constant

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.constant.AnnotationValue
import org.jetbrains.kotlin.constant.ConstantValue
import org.jetbrains.kotlin.constant.ErrorValue
import org.jetbrains.kotlin.constant.KClassValue
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.runIf

internal fun Map<Name, FirExpression>.convertToConstantValues(
    session: FirSession,
    constValueProvider: ConstValueProvider?
): Map<Name, ConstantValue<*>> {
    return this.map { (name, firExpression) ->
        val constantValue = constValueProvider?.findConstantValueFor(firExpression)
            ?: firExpression.toConstantValue(session, constValueProvider)
            ?: runIf(session.languageVersionSettings.getFlag(AnalysisFlags.metadataCompilation)) {
                ErrorValue.ErrorValueWithMessage("Constant conversion can be ignored in metadata compilation mode")
            }
            ?: error("Cannot convert expression ${firExpression.render()} to constant")
        name to constantValue
    }.toMap()
}

internal fun LinkedHashMap<FirExpression, FirValueParameter>.convertToConstantValues(
    session: FirSession,
    constValueProvider: ConstValueProvider?,
): Map<Name, ConstantValue<*>> {
    return this.map { (firExpression, firValueParameter) -> firValueParameter.name to firExpression }
        .toMap().convertToConstantValues(session, constValueProvider)
}

inline fun <reified T : ConeKotlinType> AnnotationValue.coneTypeSafe(): T? {
    return this.value.type as? T
}

inline fun <reified T : ConeKotlinType> KClassValue.Value.LocalClass.coneType(): T {
    return this.type as T
}

internal fun KClassValue.getArgumentType(session: FirSession): ConeKotlinType? {
    when (val castedValue = value) {
        is KClassValue.Value.LocalClass -> return castedValue.type as ConeKotlinType
        is KClassValue.Value.NormalClass -> {
            val (classId, arrayDimensions) = castedValue.value
            val klass = session.symbolProvider.getClassLikeSymbolByClassId(classId)?.fir as? FirRegularClass ?: return null
            var type: ConeClassLikeType = klass.defaultType().replaceArgumentsWithStarProjections()
            repeat(arrayDimensions) {
                type = type.createArrayType()
            }
            return type
        }
    }
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
