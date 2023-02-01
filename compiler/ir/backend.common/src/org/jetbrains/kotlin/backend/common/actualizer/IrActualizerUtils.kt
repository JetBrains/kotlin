/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.actualizer

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.FqName

fun generateIrElementFullName(
    declaration: IrElement,
    expectActualTypesMap: Map<IrSymbol, IrSymbol>,
    typeAliasMap: Map<FqName, FqName>? = null
): String {
    return StringBuilder().apply { appendElementFullName(declaration, expectActualTypesMap, this, typeAliasMap) }.toString()
}

private fun appendElementFullName(
    declaration: IrElement,
    expectActualTypesMap: Map<IrSymbol, IrSymbol>,
    result: StringBuilder,
    typeAliasMap: Map<FqName, FqName>? = null
) {
    if (declaration !is IrDeclarationBase) return

    val parentName = declaration.parent.kotlinFqName
    if (parentName.asString().isNotEmpty()) {
        result.append(typeAliasMap?.get(parentName) ?: parentName.asString())
        result.append('.')
    }

    if (declaration is IrDeclarationWithName) {
        result.append(declaration.name)
    }

    if (declaration is IrFunction) {
        fun appendType(type: IrType) {
            val typeClassifier = type.classifierOrFail
            val actualizedTypeSymbol = expectActualTypesMap[typeClassifier] ?: typeClassifier
            appendElementFullName(actualizedTypeSymbol.owner, expectActualTypesMap, result)
        }

        val extensionReceiverType = declaration.extensionReceiverParameter?.type
        if (extensionReceiverType != null) {
            result.append('[')
            appendType(extensionReceiverType)
            result.append(']')
        }

        result.append('(')
        for ((index, parameter) in declaration.valueParameters.withIndex()) {
            appendType(parameter.type)
            if (index < declaration.valueParameters.size - 1) {
                result.append(',')
            }
        }
        result.append(')')
    }
}

fun reportMissingActual(irElement: IrElement) {
    // TODO: setup diagnostics reporting
    throw AssertionError("Missing actual for ${irElement.render()}")
}

fun reportManyInterfacesMembersNotImplemented(declaration: IrClass, actualMember: IrDeclarationWithName) {
    // TODO: setup diagnostics reporting
    throw AssertionError("${declaration.name} must override ${actualMember.name} because it inherits multiple interface methods of it")
}