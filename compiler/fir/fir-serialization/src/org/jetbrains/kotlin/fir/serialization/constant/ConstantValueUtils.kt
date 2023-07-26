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

internal fun Map<Name, FirExpression>.convertToConstantValues(
    session: FirSession,
    constValueProvider: ConstValueProvider?
): MutableMap<Name, ConstantValue<*>> {
    return this.mapValuesTo(mutableMapOf()) { (_, firExpression) ->
        constValueProvider?.findConstantValueFor(firExpression)
            ?: firExpression.toConstantValue(session, constValueProvider)
            ?: runIf(session.languageVersionSettings.getFlag(AnalysisFlags.metadataCompilation)) {
                ErrorValue.ErrorValueWithMessage("Constant conversion can be ignored in metadata compilation mode")
            }
            ?: error("Cannot convert expression ${firExpression.render()} to constant")
    }
}

internal fun MutableMap<Name, ConstantValue<Any?>>.addEmptyVarargValuesFor(
    symbol: FirFunctionSymbol<*>?,
): MutableMap<Name, ConstantValue<Any?>> = apply {
    if (symbol == null) return@apply
    for ((i, parameter) in symbol.valueParameterSymbols.withIndex()) {
        if (parameter.name !in this && parameter.isVararg && !symbol.fir.itOrExpectHasDefaultParameterValue(i)) {
            this[parameter.name] = ArrayValue(emptyList())
        }
    }
}

internal fun LinkedHashMap<FirExpression, FirValueParameter>.convertToConstantValues(
    session: FirSession,
    constValueProvider: ConstValueProvider?,
): MutableMap<Name, ConstantValue<*>> {
    return this.entries.associate { (firExpression, firValueParameter) -> firValueParameter.name to firExpression }
        .convertToConstantValues(session, constValueProvider)
}

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
