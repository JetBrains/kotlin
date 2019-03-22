/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class JvmSymbols(
    context: JvmBackendContext,
    private val symbolTable: ReferenceSymbolTable
) : Symbols<JvmBackendContext>(context, symbolTable) {
    override val ThrowNullPointerException: IrSimpleFunctionSymbol
        get() = error("Unused in JVM IR")

    override val ThrowNoWhenBranchMatchedException: IrSimpleFunctionSymbol
        get() = error("Unused in JVM IR")

    override val ThrowTypeCastException: IrSimpleFunctionSymbol
        get() = error("Unused in JVM IR")

    override val ThrowUninitializedPropertyAccessException: IrSimpleFunctionSymbol =
        symbolTable.referenceSimpleFunction(
            context.getJvmInternalClass("Intrinsics").staticScope.getContributedFunctions(
                Name.identifier("throwUninitializedPropertyAccessException"),
                NoLookupLocation.FROM_BACKEND
            ).single()
        )

    override val stringBuilder: IrClassSymbol
        get() = symbolTable.referenceClass(context.getClass(FqName("java.lang.StringBuilder")))

    override val defaultConstructorMarker: IrClassSymbol =
        symbolTable.referenceClass(context.getJvmInternalClass("DefaultConstructorMarker"))

    override val copyRangeTo: Map<ClassDescriptor, IrSimpleFunctionSymbol>
        get() = error("Unused in JVM IR")

    override val coroutineImpl: IrClassSymbol
        get() = TODO("not implemented")

    override val coroutineSuspendedGetter: IrSimpleFunctionSymbol
        get() = TODO("not implemented")

    val lambdaClass: IrClassSymbol =
        symbolTable.referenceClass(context.getJvmInternalClass("Lambda"))

    val functionReference: IrClassSymbol =
        symbolTable.referenceClass(context.getJvmInternalClass("FunctionReference"))

    fun getFunction(parameterCount: Int): IrClassSymbol =
        symbolTable.referenceClass(context.builtIns.getFunction(parameterCount))
}

