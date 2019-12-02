/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.SimpleType

class WasmSymbols(
    context: WasmBackendContext,
    private val symbolTable: SymbolTable
) : Symbols<WasmBackendContext>(context, symbolTable) {

    override val ThrowNullPointerException
        get() = TODO()
    override val ThrowNoWhenBranchMatchedException
        get() = TODO()
    override val ThrowTypeCastException
        get() = TODO()
    override val ThrowUninitializedPropertyAccessException
        get() = TODO()
    override val defaultConstructorMarker
        get() = TODO()
    override val stringBuilder
        get() = TODO()
    override val copyRangeTo: Map<ClassDescriptor, IrSimpleFunctionSymbol>
        get() = TODO()
    override val coroutineImpl
        get() = TODO()
    override val coroutineSuspendedGetter
        get() = TODO()
    override val getContinuation
        get() = TODO()
    override val coroutineContextGetter by lazy {
        context.excludedDeclarations.addFunction {
            name = Name.identifier("coroutineContextGetter\$Stub")
        }.symbol
    }

    override val suspendCoroutineUninterceptedOrReturn
        get() = TODO()
    override val coroutineGetContext
        get() = TODO()
    override val returnIfSuspended
        get() = TODO()

    private val wasmInternalPackage = context.module.getPackage(FqName("kotlin.wasm.internal"))

    val equalityFunctions = mapOf(
        context.irBuiltIns.booleanType to getInternalFunction("wasm_i32_eq"),
        context.irBuiltIns.byteType to getInternalFunction("wasm_i32_eq"),
        context.irBuiltIns.shortType to getInternalFunction("wasm_i32_eq"),
        context.irBuiltIns.charType to getInternalFunction("wasm_i32_eq"),
        context.irBuiltIns.intType to getInternalFunction("wasm_i32_eq"),
        context.irBuiltIns.longType to getInternalFunction("wasm_i64_eq"),
        context.irBuiltIns.floatType to getInternalFunction("wasm_f32_eq"),
        context.irBuiltIns.doubleType to getInternalFunction("wasm_f64_eq")
    )

    private fun wasmString(classfier: IrClassifierSymbol): String = with(context.irBuiltIns) {
        when (classfier) {
            booleanClass, byteClass, shortClass, charClass, intClass -> "i32"
            floatClass -> "f32"
            doubleClass -> "f64"
            longClass -> "i64"
            else -> error("Unknown primitive type")
        }
    }

    val irBuiltInsToWasmIntrinsics = context.irBuiltIns.run {
        mapOf(
            lessFunByOperandType to "lt",
            lessOrEqualFunByOperandType to "le",
            greaterOrEqualFunByOperandType to "ge",
            greaterFunByOperandType to "gt"
        ).map { (typeToBuiltIn, wasmOp) ->
            typeToBuiltIn.map { (type, builtin) ->
                val wasmType = wasmString(type)
                val markSign = if (wasmType == "i32" || wasmType == "i64") "_s" else ""
                builtin to getInternalFunction("wasm_${wasmType}_$wasmOp$markSign")
            }
        }.flatten().toMap()
    }

    val stringGetLiteral = getInternalFunction("stringLiteral")

    private fun findClass(memberScope: MemberScope, name: Name): ClassDescriptor =
        memberScope.getContributedClassifier(name, NoLookupLocation.FROM_BACKEND) as ClassDescriptor

    private fun findFunctions(memberScope: MemberScope, name: Name): List<SimpleFunctionDescriptor> =
        memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BACKEND).toList()

    private fun findProperty(memberScope: MemberScope, name: Name): List<PropertyDescriptor> =
        memberScope.getContributedVariables(name, NoLookupLocation.FROM_BACKEND).toList()

    internal fun getClass(fqName: FqName): ClassDescriptor =
        findClass(context.module.getPackage(fqName.parent()).memberScope, fqName.shortName())

    internal fun getProperty(fqName: FqName): PropertyDescriptor =
        findProperty(context.module.getPackage(fqName.parent()).memberScope, fqName.shortName()).single()

    internal fun getInternalFunction(name: String): IrSimpleFunctionSymbol {
        val tmp = findFunctions(wasmInternalPackage.memberScope, Name.identifier(name)).single()
        return symbolTable.referenceSimpleFunction(tmp)
    }

    private fun getIrClass(fqName: FqName): IrClassSymbol = symbolTable.referenceClass(getClass(fqName))
}