/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.render

fun checkParameters(
    expectFunction: IrFunction,
    actualFunction: IrFunction,
    expectActualTypesMap: Map<IrSymbol, IrSymbol>
): Boolean {
    if (expectFunction.valueParameters.size != actualFunction.valueParameters.size) return false
    for ((expectParameter, actualParameter) in expectFunction.valueParameters.zip(actualFunction.valueParameters)) {
        val expectParameterTypeSymbol = expectParameter.type.classifierOrFail
        val actualizedParameterTypeSymbol = expectActualTypesMap[expectParameterTypeSymbol] ?: expectParameterTypeSymbol
        if (actualizedParameterTypeSymbol != actualParameter.type.classifierOrFail) {
            return false
        }
    }
    return true
}

fun reportMissingActual(irElement: IrElement) {
    // TODO: set up diagnostics reporting
    throw AssertionError("Missing actual for ${irElement.render()}")
}

fun reportManyInterfacesMembersNotImplemented(declaration: IrClass, actualMember: IrDeclarationWithName) {
    // TODO: set up diagnostics reporting
    throw AssertionError("${declaration.name} must override ${actualMember.name} because it inherits multiple interface methods of it")
}