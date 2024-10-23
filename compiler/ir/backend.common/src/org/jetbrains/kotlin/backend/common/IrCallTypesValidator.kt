/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.isSubtypeOf
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * Makes sure that: [IrVararg.type] is an array of [IrVararg.varargElementType].
 */
internal class IrCallTypesValidator(
    private val irBuiltIns: IrBuiltIns,
    private val file: IrFile,
    private val config: IrValidatorConfig,
    private val reportError: ReportIrValidationError,
) : IrElementVisitorVoid {
    private val parentChain = mutableListOf<IrElement>()
    private val typeSystem = IrTypeSystemContextImpl(irBuiltIns)

    override fun visitElement(element: IrElement) {
        parentChain.push(element)
        element.acceptChildrenVoid(this)
        parentChain.pop()
    }

    override fun visitCall(irCall: IrCall, data: Nothing?) {
        if (irCall.type.isSubtypeOf(irBuiltIns.unitType, typeSystem))
            return
        if ((irCall.type as? IrSimpleType)?.arguments?.isNotEmpty() == true) {
            // TODO: also check types with parameters. Trouble: type substitution is needed, e.g
            // val t: T2 = foo() // type argument 0 - T2. type parameter 0 - T. Return type - T. Expected type - T2
            // substitute(returnType, { T = T2 }) => T2
            // T2 <: T2 // OK
            return
        }
        // TODO: check type parameters. Trouble: type substitution is needed
        if (irCall.type.classifierOrNull is IrTypeParameterSymbol)
            return
        val returnType = irCall.symbol.owner.returnType
        val returnTypeClassifier = returnType.classifierOrNull
        if (returnTypeClassifier is IrTypeParameterSymbol)
            return
        if (returnTypeClassifier == irBuiltIns.anyType.classifierOrNull) // probably, intrinsics returning Any
            return

        if (returnType.isSubtypeOf(irCall.type, typeSystem))
            return
        if (returnType.isSubtypeOf(irCall.type, typeSystem))
            return

        reportError(
            file,
            irCall,
            "Found irCall with mismatching `type` and `symbol.owner.returnType`: ${irCall.render()}",
            parentChain
        )
    }
}
