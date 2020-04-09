/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.FirLoopTarget
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.builder.FirFunctionCallBuilder
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class Context<T> {
    lateinit var packageFqName: FqName
    var className: FqName = FqName.ROOT
    val currentClassId get() = ClassId(packageFqName, className, firFunctionTargets.isNotEmpty())

    val firFunctionTargets = mutableListOf<FirFunctionTarget>()
    val firFunctionCalls = mutableListOf<FirFunctionCallBuilder>()
    val firLabels = mutableListOf<FirLabel>()
    val firLoopTargets = mutableListOf<FirLoopTarget>()
    var capturedTypeParameters = persistentListOf<FirTypeParameterSymbol>()
    val arraySetArgument = mutableMapOf<T, FirExpression>()
}