/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.DescriptorMetadataSource
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal

@ObsoleteDescriptorBasedAPI
fun SymbolTable.declareSimpleFunctionWithOverrides(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    descriptor: FunctionDescriptor
) =
    declareSimpleFunction(descriptor) {
        with(descriptor) {
            irFactory.createFunction(
                startOffset, endOffset, origin, it, nameProvider.nameForDeclaration(this),
                visibility, modality, IrUninitializedType, isInline, isEffectivelyExternal(), isTailrec, isSuspend, isOperator, isInfix, isExpect
            ).also { declaration ->
                declaration.metadata = DescriptorMetadataSource.Function(this)
            }
        }
    }.also { declaration ->
        generateOverriddenFunctionSymbols(declaration, this)
    }

@ObsoleteDescriptorBasedAPI
fun generateOverriddenFunctionSymbols(
    declaration: IrSimpleFunction,
    symbolTable: ReferenceSymbolTable
) {
    declaration.overriddenSymbols = declaration.descriptor.overriddenDescriptors.map {
        symbolTable.referenceSimpleFunction(it.original)
    }
}
