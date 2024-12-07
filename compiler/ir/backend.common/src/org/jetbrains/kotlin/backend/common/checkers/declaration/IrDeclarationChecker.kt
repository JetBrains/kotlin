/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.checkers.declaration

import org.jetbrains.kotlin.backend.common.checkers.context.CheckerContext
import org.jetbrains.kotlin.ir.declarations.*

internal interface IrDeclarationChecker<in D : IrDeclaration> {
    fun check(declaration: D, context: CheckerContext)
}

internal fun <D : IrDeclaration> List<IrDeclarationChecker<D>>.check(declaration: D, context: CheckerContext) {
    for (checker in this) {
        checker.check(declaration, context)
    }
}

internal typealias IrValueParameterChecker = IrDeclarationChecker<IrValueParameter>
internal typealias IrFieldChecker = IrDeclarationChecker<IrField>
internal typealias IrFunctionChecker = IrDeclarationChecker<IrFunction>