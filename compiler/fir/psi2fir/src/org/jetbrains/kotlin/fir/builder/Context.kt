/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.fir.FirLabel
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class Context {
    lateinit var packageFqName: FqName
    var className: FqName = FqName.ROOT
    val currentClassId get() = ClassId(packageFqName, className, firFunctions.isNotEmpty())

    val firFunctions = mutableListOf<FirFunction<*>>()
    val firFunctionCalls = mutableListOf<FirFunctionCall>()
    val firLabels = mutableListOf<FirLabel>()
    val firLoops = mutableListOf<FirLoop>()
}