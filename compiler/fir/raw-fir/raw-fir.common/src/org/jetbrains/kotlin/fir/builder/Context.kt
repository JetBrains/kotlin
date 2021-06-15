/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirLoopTarget
import org.jetbrains.kotlin.fir.FirSourceElementKind
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class Context<T> {
    lateinit var packageFqName: FqName
    var className: FqName = FqName.ROOT
    var forcedLocalContext: Boolean = false
    val inLocalContext get() = forcedLocalContext || firFunctionTargets.isNotEmpty()
    val currentClassId get() = ClassId(packageFqName, className, inLocalContext)

    val firFunctionTargets = mutableListOf<FirFunctionTarget>()
    val calleeNamesForLambda = mutableListOf<Name>()
    val firLabels = mutableListOf<FirLabel>()
    val firLoopTargets = mutableListOf<FirLoopTarget>()
    val capturedTypeParameters = mutableListOf<StatusFirTypeParameterSymbolList>()
    val arraySetArgument = mutableMapOf<T, FirExpression>()

    var forcedElementSourceKind: FirSourceElementKind? = null
    val dispatchReceiverTypesStack = mutableListOf<ConeClassLikeType>()

    fun pushFirTypeParameters(notNested: Boolean, parameters: List<FirTypeParameterRef>) {
        capturedTypeParameters.add(StatusFirTypeParameterSymbolList(notNested, parameters.map { it.symbol }))
    }

    fun popFirTypeParameters() {
        val list = capturedTypeParameters
        list.removeAt(list.lastIndex)
    }

    inline fun applyToActualCapturedTypeParameters(ignoreLastLevel: Boolean, action: (FirTypeParameterSymbol) -> Unit) {
        for (index in capturedTypeParameters.lastIndex downTo 0) {
            val element = capturedTypeParameters[index]

            if (index < capturedTypeParameters.lastIndex || !ignoreLastLevel) {
                for (capturedTypeParameter in element.list) {
                    action(capturedTypeParameter)
                }
            }

            if (!element.notNested) {
                break
            }
        }
    }

    data class StatusFirTypeParameterSymbolList(val notNested: Boolean, val list: List<FirTypeParameterSymbol> = listOf())
}
