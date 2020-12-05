/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.util.OperatorNameConventions

class WasmSymbols(
    context: WasmBackendContext,
    private val symbolTable: SymbolTable
) : Symbols<WasmBackendContext>(context, context.irBuiltIns, symbolTable) {

    private val wasmInternalPackage: PackageViewDescriptor =
        context.module.getPackage(FqName("kotlin.wasm.internal"))

    override val throwNullPointerException = getInternalFunction("THROW_NPE")
    override val throwISE = getInternalFunction("THROW_ISE")
    override val throwNoWhenBranchMatchedException = throwISE
    override val throwTypeCastException = getInternalFunction("THROW_CCE")
    override val throwUninitializedPropertyAccessException =
        getInternalFunction("throwUninitializedPropertyAccessException")
    override val defaultConstructorMarker =
        getIrClass(FqName("kotlin.wasm.internal.DefaultConstructorMarker"))
    override val throwKotlinNothingValueException: IrSimpleFunctionSymbol
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
        context.irFactory.addFunction(context.getExcludedPackageFragment(FqName("kotlin.excluded"))) {
            name = Name.identifier("coroutineContextGetter\$Stub")
        }.symbol
    }

    override val suspendCoroutineUninterceptedOrReturn
        get() = TODO()
    override val coroutineGetContext
        get() = TODO()
    override val returnIfSuspended
        get() = TODO()

    override val functionAdapter: IrClassSymbol
        get() = TODO()

    val wasmUnreachable = getInternalFunction("wasm_unreachable")
    val wasmFloatNaN = getInternalFunction("wasm_float_nan")
    val wasmDoubleNaN = getInternalFunction("wasm_double_nan")

    val equalityFunctions = mapOf(
        context.irBuiltIns.booleanType to getInternalFunction("wasm_i32_eq"),
        context.irBuiltIns.byteType to getInternalFunction("wasm_i32_eq"),
        context.irBuiltIns.shortType to getInternalFunction("wasm_i32_eq"),
        context.irBuiltIns.charType to getInternalFunction("wasm_i32_eq"),
        context.irBuiltIns.intType to getInternalFunction("wasm_i32_eq"),
        context.irBuiltIns.longType to getInternalFunction("wasm_i64_eq"),
        context.irBuiltIns.stringType to getInternalFunction("wasm_string_eq")
    )

    val floatEqualityFunctions = mapOf(
        context.irBuiltIns.floatType to getInternalFunction("wasm_f32_eq"),
        context.irBuiltIns.doubleType to getInternalFunction("wasm_f64_eq")
    )

    private fun wasmPrimitiveTypeName(classifier: IrClassifierSymbol): String = with(context.irBuiltIns) {
        when (classifier) {
            booleanClass, byteClass, shortClass, charClass, intClass -> "i32"
            floatClass -> "f32"
            doubleClass -> "f64"
            longClass -> "i64"
            else -> error("Unknown primitive type")
        }
    }

    val comparisonBuiltInsToWasmIntrinsics = context.irBuiltIns.run {
        listOf(
            lessFunByOperandType to "lt",
            lessOrEqualFunByOperandType to "le",
            greaterOrEqualFunByOperandType to "ge",
            greaterFunByOperandType to "gt"
        ).map { (typeToBuiltIn, wasmOp) ->
            typeToBuiltIn.map { (type, builtin) ->
                val wasmType = wasmPrimitiveTypeName(type)
                val markSign = if (wasmType == "i32" || wasmType == "i64") "_s" else ""
                builtin to getInternalFunction("wasm_${wasmType}_$wasmOp$markSign")
            }
        }.flatten().toMap()
    }

    val booleanAnd = getInternalFunction("wasm_i32_and")
    val refEq = getInternalFunction("wasm_ref_eq")
    val refIsNull = getInternalFunction("wasm_ref_is_null")
    val intToLong = getInternalFunction("wasm_i64_extend_i32_s")

    val wasmRefCast = getInternalFunction("wasm_ref_cast")

    val boxIntrinsic: IrSimpleFunctionSymbol = getInternalFunction("boxIntrinsic")
    val unboxIntrinsic: IrSimpleFunctionSymbol = getInternalFunction("unboxIntrinsic")

    val stringGetLiteral = getInternalFunction("stringLiteral")

    val wasmClassId = getInternalFunction("wasmClassId")
    val wasmInterfaceId = getInternalFunction("wasmInterfaceId")

    val getVirtualMethodId = getInternalFunction("getVirtualMethodId")
    val getInterfaceMethodId = getInternalFunction("getInterfaceMethodId")

    val isSubClass = getInternalFunction("isSubClass")
    val isInterface = getInternalFunction("isInterface")

    val nullableEquals = getInternalFunction("nullableEquals")
    val ensureNotNull = getInternalFunction("ensureNotNull")
    val anyNtoString = getInternalFunction("anyNtoString")

    val nullableFloatIeee754Equals = getInternalFunction("nullableFloatIeee754Equals")
    val nullableDoubleIeee754Equals = getInternalFunction("nullableDoubleIeee754Equals")

    val wasmThrow = getInternalFunction("wasmThrow")

    private val functionNInterfaces = (0..22).map { arity ->
        getIrClass(FqName("kotlin.wasm.internal.Function$arity"))
    }

    val functionNInvokeMethods by lazy {
        functionNInterfaces.map { interfaceSymbol ->
            interfaceSymbol.owner.declarations.filterIsInstance<IrSimpleFunction>().single { method ->
                method.name == OperatorNameConventions.INVOKE
            }.symbol
        }
    }

    override fun functionN(n: Int): IrClassSymbol =
        functionNInterfaces[n]

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

    private fun getInternalFunction(name: String): IrSimpleFunctionSymbol {
        val tmp = findFunctions(wasmInternalPackage.memberScope, Name.identifier(name)).single()
        return symbolTable.referenceSimpleFunction(tmp)
    }

    private fun getIrClass(fqName: FqName): IrClassSymbol = symbolTable.referenceClass(getClass(fqName))
}