/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.utils

import org.jetbrains.kotlin.descriptors.isInterface
import org.jetbrains.kotlin.descriptors.isClass
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.util.parentClassOrNull

fun IrDeclaration.isExportedMember() =
    parentClassOrNull.let { it is IrClass && it.isJsExport() }

fun IrDeclaration?.isExportedClass() =
    this is IrClass && kind.isClass && isJsExport()

fun IrDeclaration?.isExportedInterface() =
    this is IrClass && kind.isInterface && isJsExport()

fun IrDeclaration.isExportedInterfaceMember() =
    parentClassOrNull.isExportedInterface()

fun IrReturn.isTheLastReturnStatementIn(target: IrReturnableBlockSymbol): Boolean {
    return target.owner.statements.lastOrNull() === this
}